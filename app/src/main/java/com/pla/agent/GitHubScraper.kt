package com.pla.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class GitHubScraper {
    private val client = OkHttpClient()

    // Mencari repositori di GitHub berdasarkan kata kunci
    suspend fun cariReferensi(kataKunci: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(kataKunci, "UTF-8")
            val url = "https://api.github.com/search/repositories?q=$encodedQuery&sort=stars&order=desc&per_page=3"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "PLA-Agent-App")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext "Tidak ada hasil."
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                
                if (items.length() == 0) {
                    return@withContext "Tidak ditemukan repositori yang cocok."
                }

                val hasil = StringBuilder()
                for (i in 0 until items.length()) {
                    val repo = items.getJSONObject(i)
                    val name = repo.getString("full_name")
                    val desc = repo.optString("description", "Tanpa deskripsi")
                    val stars = repo.getInt("stargazers_count")
                    val htmlUrl = repo.getString("html_url")
                    
                    hasil.append("- Repositori: $name (⭐ $stars)\n")
                    hasil.append("  Deskripsi: $desc\n")
                    hasil.append("  Link: $htmlUrl\n\n")
                }
                return@withContext hasil.toString()
            } else {
                return@withContext "Gagal mengakses GitHub. Kode: ${response.code} (Mungkin limit API tercapai)."
            }
        } catch (e: Exception) {
            return@withContext "Error saat mencari di GitHub: ${e.message}"
        }
    }
}