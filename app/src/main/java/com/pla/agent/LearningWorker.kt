package com.pla.agent

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LearningWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val isScreenOn = powerManager.isInteractive
            val isAudioPlaying = audioManager.isMusicActive

            // Jangan belajar jika HP sedang dipakai
            if (isScreenOn || isAudioPlaying) {
                return@withContext Result.retry()
            }

            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "pla-agent-db"
            ).fallbackToDestructiveMigration().build()

            // Dapatkan versi AI yang sedang aktif
            val activeVersion = database.appDao().getActiveVersion()?.versionName ?: "V1. pla"
            val client = OkHttpClient()

            var url = ""
            when {
                activeVersion.contains("Coding", ignoreCase = true) -> {
                    url = "https://api.github.com/search/repositories?q=android+kotlin&sort=stars&order=desc"
                }
                else -> {
                    url = "https://id.wikipedia.org/api/rest_v1/page/random/summary"
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; PLA-Agent/1.0)")
                .build()

            val response = client.newCall(request).execute()
            var learnedText = ""

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                if (activeVersion.contains("Coding", ignoreCase = true)) {
                    val json = JSONObject(bodyString)
                    val items = json.getJSONArray("items")
                    if (items.length() > 0) {
                        val repo = items.getJSONObject(0)
                        learnedText = "Repo GitHub: ${repo.getString("name")} - ${repo.optString("description", "Tidak ada deskripsi")}"
                    }
                } else {
                    val json = JSONObject(bodyString)
                    learnedText = "Wikipedia (${json.optString("title")}): ${json.optString("extract")}"
                }
            }

            if (learnedText.isNotEmpty()) {
                // Simpan ke database dengan versi dan sumber URL
                database.appDao().insertMemory(
                    MemoryEntity(
                        versionName = activeVersion, 
                        sourceUrl = url,
                        content = learnedText
                    )
                )
            }

            database.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}