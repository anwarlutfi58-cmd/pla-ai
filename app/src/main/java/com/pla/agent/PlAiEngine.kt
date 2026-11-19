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
        
        val activeVersion = appDao.getActiveVersion()?.versionName ?: "V1. pla"
        val aiName = preferences.getAiName()
        
        onStatusUpdate("Menganalisis dengan $activeVersion...")
        
        val lowerInput = userInput.lowercase().trim()

        // ==========================================
        // 1. LOGIKA SAPAAN & CUSTOM RULES (SMALL TALK)
        // ==========================================
        val greetingResponse = when {
            // Aturan Khusus: Jika user mengetik "apa"
            lowerInput == "apa" || lowerInput == "apa?" -> 
                "Assalamualaikum, ada yang bisa saya bantu?"
            
            // Aturan Khusus: Jika user menyapa dengan salam
            lowerInput.contains("assalamualaikum") || lowerInput.contains("assalamu alaikum") -> 
                "Waalaikumsalam warahmatullahi wabarakatuh. Ada yang bisa saya bantu?"
            
            lowerInput == "halo" || lowerInput == "hai" || lowerInput == "hello" -> 
                "Halo! Saya $aiName. Ada yang bisa saya bantu hari ini?"
            
            lowerInput.contains("selamat pagi") -> 
                "Selamat pagi! Semoga harimu menyenangkan. Ada yang bisa saya bantu?"
            lowerInput.contains("selamat siang") -> 
                "Selamat siang! Ada yang bisa saya bantu?"
            lowerInput.contains("selamat malam") -> 
                "Selamat malam! Ada yang bisa saya bantu?"
            lowerInput.contains("terima kasih") -> 
                "Sama-sama! Senang bisa membantu Anda."
            lowerInput.contains("siapa kamu") || lowerInput.contains("siapa nama kamu") -> 
                "Saya $aiName, asisten AI mandiri yang siap membantu Anda belajar."
            else -> null
        }

        // Jika cocok dengan aturan di atas, langsung jawab tanpa mencari ke database
        if (greetingResponse != null) {
            for (char in greetingResponse) {
                onToken(char.toString())
                Thread.sleep(20)
            }
            return@withContext
        }

        // ==========================================
        // 2. PENCARIAN DI DATABASE (PENGETAHUAN JSON)
        // ==========================================
        val inputTokens = analyzer.tokenize(userInput)
        
        if (inputTokens.isEmpty()) {
            onToken("Silakan ketik pertanyaan Anda.")
            return@withContext
        }

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

        for (char in finalResponse) {
            onToken(char.toString())
            Thread.sleep(20)
        }
    }
}