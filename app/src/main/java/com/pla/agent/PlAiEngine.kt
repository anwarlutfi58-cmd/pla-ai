package com.pla.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlAiEngine(
    private val context: Context,
    private val appDao: AppDao
) {
    private val analyzer = LanguageAnalyzer()
    private val preferences = AppPreferences(context)

    fun isLoaded(): Boolean = true
    fun unloadModel() {}

    suspend fun generateResponse(
        userInput: String, 
        onStatusUpdate: (String) -> Unit,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        // Ambil versi yang sedang aktif
        val activeVersion = appDao.getActiveVersion()?.versionName ?: "V1. pla"
        val aiName = preferences.getAiName()
        
        onStatusUpdate("Menganalisis dengan $activeVersion...")
        
        val inputTokens = analyzer.tokenize(userInput)
        
        if (inputTokens.isEmpty()) {
            onToken("Halo! Ada yang bisa saya bantu?")
            return@withContext
        }

        // Ambil ingatan dari versi yang aktif saja
        val memories = appDao.getMemoriesByVersion(activeVersion)
        
        var bestMatchContent = ""
        var highestScore = 0

        for (memory in memories) {
            val score = analyzer.hitungSkorKecocokan(inputTokens, memory.content)
            if (score > highestScore) {
                highestScore = score
                bestMatchContent = memory.content
            }
        }

        var finalResponse = ""

        if (highestScore > 0) {
            finalResponse = bestMatchContent
                .replace("{", "").replace("}", "")
                .replace("\"", "").replace("id:", "")
                .replace("topik:", "").replace("kategori:", "")
                .replace("isi:", "")
                .trim()
            onStatusUpdate("Ingatan ditemukan di $activeVersion")
        } else {
            finalResponse = "Maaf, saya belum mempelajari hal itu di versi $activeVersion. Silakan tambahkan link data di Pengaturan."
        }

        val completeResponse = finalResponse
        for (char in completeResponse) {
            onToken(char.toString())
            Thread.sleep(20)
        }
    }
}