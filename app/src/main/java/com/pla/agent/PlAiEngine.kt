package com.pla.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlAiEngine(
    private val context: Context,
    private val appDao: AppDao
) {
    private val analyzer = LanguageAnalyzer()
    private val githubScraper = GitHubScraper()
    private val preferences = AppPreferences(context)

    // AI selalu dianggap siap karena tidak butuh model eksternal
    fun isLoaded(): Boolean = true 
    fun unloadModel() {} // Dikosongkan karena tidak ada model

    suspend fun generateResponse(
        userInput: String, 
        onStatusUpdate: (String) -> Unit,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        val activeTopic = appDao.getActiveTopic()?.name ?: "Umum"
        val aiName = preferences.getAiName()
        
        onStatusUpdate("Menganalisis bahasa...")
        
        // 1. Pecah pertanyaan user menjadi kata kunci
        val inputTokens = analyzer.tokenize(userInput)
        
        if (inputTokens.isEmpty()) {
            onToken("Halo! Ada yang bisa saya bantu?")
            return@withContext
        }

        // 2. Ambil semua ingatan dari Database
        val memories = appDao.getMemoriesByTopic(activeTopic)
        
        var bestMatchContent = ""
        var highestScore = 0

        // 3. Cari ingatan yang paling cocok
        for (memory in memories) {
            val score = analyzer.hitungSkorKecocokan(inputTokens, memory.content)
            if (score > highestScore) {
                highestScore = score
                bestMatchContent = memory.content
            }
        }

        // 4. Susun Jawaban
        var finalResponse = ""

        if (highestScore > 0) {
            // Jika ada kecocokan di database
            // Ambil teks aslinya (jika formatnya JSON, kita bersihkan sedikit)
            finalResponse = bestMatchContent
                .replace("{", "").replace("}", "")
                .replace("\"", "").replace("id:", "")
                .replace("topik:", "").replace("kategori:", "")
                .replace("isi:", "")
                .trim()
            
            onStatusUpdate("Ingatan ditemukan!")
        } else {
            // Jika tidak ada di database, coba cari di GitHub (jika topik Coding)
            if (activeTopic.contains("Coding", ignoreCase = true) && preferences.isInternetEnabled()) {
                onStatusUpdate("Mencari referensi di GitHub...")
                val githubRef = githubScraper.cariReferensi(userInput)
                finalResponse = "Saya belum mempelajari hal itu secara spesifik. Namun, ini referensi dari GitHub:\n\n$githubRef"
            } else {
                finalResponse = "Maaf, saya belum mempelajari topik ini. Tolong tambahkan pengetahuannya di menu Pengaturan."
            }
        }

        // 5. Ketik jawaban huruf per huruf (Simulasi)
        val completeResponse = "$aiName: $finalResponse"
        for (char in completeResponse) {
            onToken(char.toString())
            Thread.sleep(20) // Efek mengetik
        }
    }
}