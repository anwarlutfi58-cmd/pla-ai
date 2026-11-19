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
            // ==========================================
            // SMART IDLE DETECTION (CEK APAKAH USER SEDANG PAKAI HP)
            // ==========================================
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val isScreenOn = powerManager.isInteractive
            val isAudioPlaying = audioManager.isMusicActive // Deteksi TikTok/Video/Telepon

            if (isScreenOn || isAudioPlaying) {
                // Jika user sedang main HP atau ada suara, BATALKAN proses belajar.
                // Coba lagi nanti.
                return@withContext Result.retry()
            }

            // ==========================================
            // PROSES BELAJAR (HANYA JIKA HP TIDAK DIPAKAI)
            // ==========================================
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "pla-agent-db"
            ).fallbackToDestructiveMigration().build()

            val activeTopic = database.appDao().getActiveTopic()?.name ?: "Umum"
            val client = OkHttpClient()

            var url = ""
            when {
                activeTopic.contains("Coding", ignoreCase = true) -> {
                    url = "https://api.github.com/search/repositories?q=android+kotlin&sort=stars&order=desc"
                }
                activeTopic.contains("Umum", ignoreCase = true) -> {
                    url = "https://id.wikipedia.org/api/rest_v1/page/random/summary"
                }
                else -> {
                    url = "https://id.wikipedia.org/w/api.php?action=query&list=search&srsearch=${activeTopic.replace(" ", "+")}&format=json"
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
                if (activeTopic.contains("Coding", ignoreCase = true)) {
                    val json = JSONObject(bodyString)
                    val items = json.getJSONArray("items")
                    if (items.length() > 0) {
                        val repo = items.getJSONObject(0)
                        learnedText = "Repo GitHub: ${repo.getString("name")} - ${repo.optString("description", "Tidak ada deskripsi")}"
                    }
                } else if (activeTopic.contains("Umum", ignoreCase = true)) {
                    val json = JSONObject(bodyString)
                    learnedText = "Wikipedia (${json.optString("title")}): ${json.optString("extract")}"
                } else {
                    val json = JSONObject(bodyString)
                    val query = json.getJSONObject("query")
                    val search = query.getJSONArray("search")
                    if (search.length() > 0) {
                        learnedText = "Wikipedia (${search.getJSONObject(0).getString("title")}): ${search.getJSONObject(0).getString("snippet").replace(Regex("<[^>]*>"), "")}"
                    }
                }
            }

            if (learnedText.isNotEmpty()) {
                database.appDao().insertMemory(
                    MemoryEntity(topic = activeTopic, content = learnedText)
                )
            }

            database.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}