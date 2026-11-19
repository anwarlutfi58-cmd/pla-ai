package com.pla.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class DataImporter(private val database: AppDatabase) {

    private val client = OkHttpClient()

    suspend fun importFromGitHub(url: String, versionName: String, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Menghubungi GitHub...")
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                onProgress("Gagal: Server menolak (Kode: ${response.code})")
                return@withContext false
            }

            val body = response.body?.string() ?: return@withContext false
            onProgress("Membaca data JSON...")

            // Hapus dulu data dari URL yang sama jika pernah ditambahkan (agar tidak dobel saat update)
            database.appDao().deleteMemoriesBySource(versionName, url)

            val jsonArray = JSONArray(body)
            var count = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val content = obj.toString() 
                
                // Simpan dengan nama versi & source url
                database.appDao().insertMemory(
                    MemoryEntity(
                        versionName = versionName, 
                        sourceUrl = url,
                        content = content
                    )
                )
                count++
            }

            onProgress("Berhasil! $count data masuk ke $versionName.")
            return@withContext true

        } catch (e: Exception) {
            onProgress("Error: ${e.message}")
            return@withContext false
        }
    }
}