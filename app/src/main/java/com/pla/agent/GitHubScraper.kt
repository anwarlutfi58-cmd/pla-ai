package com.pla.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubScraper {
    private val client = OkHttpClient()

    // Fungsi untuk mengambil isi file dari GitHub (menggunakan Raw URL)
    suspend fun ambilKodeDariGitHub(urlRaw: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlRaw)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: "Gagal membaca isi file."
            } else {
                "Gagal mengambil data. Kode: ${response.code}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}