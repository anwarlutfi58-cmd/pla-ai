package com.pla.agent

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlAiEngine(
    private val context: Context,
    private val appDao: AppDao
) {
    private var isModelLoaded = false
    private var currentModelName = ""
    private val githubScraper = GitHubScraper()

    fun getAvailableModel(): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.listFiles()?.firstOrNull { it.name.endsWith(".gguf") }
    }

    suspend fun loadModel(onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val modelFile = getAvailableModel()
        if (modelFile == null) {
            onProgress("Model tidak ditemukan.")
            return@withContext false
        }

        onProgress("Memuat model ${modelFile.name}...")
        try {
            Thread.sleep(1500)
            isModelLoaded = true
            currentModelName = modelFile.name
            onProgress("Model berhasil dimuat!")
            return@withContext true
        } catch (e: Exception) {
            onProgress("Gagal memuat: ${e.message}")
            return@withContext false
        }
    }

    suspend fun generateResponse(
        userInput: String, 
        onStatusUpdate: (String) -> Unit,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            onToken("Error: Model belum dimuat.")
            return@withContext
        }

        val activeTopic = appDao.getActiveTopic()?.name ?: "Umum"
        val memories = appDao.getMemoriesByTopic(activeTopic)
        
        var githubContext = ""
        
        // DETEKSI OTOMATIS: Jika topik Coding atau ada kata kunci pemrograman
        val isCodingQuery = activeTopic.contains("Coding", ignoreCase = true) || 
                            userInput.contains("kode", ignoreCase = true) || 
                            userInput.contains("coding", ignoreCase = true) ||
                            userInput.contains("github", ignoreCase = true)

        if (isCodingQuery) {
            onStatusUpdate("Mencari referensi di GitHub...")
            // Cari referensi berdasarkan pertanyaan user
            githubContext = githubScraper.cariReferensi(userInput)
        }

        val memoryContext = memories.joinToString("\n") { "Dokumen: ${it.content.take(500)}" }

        val systemPrompt = """
            Anda adalah PLA Agent.
            Topik saat ini: $activeTopic.
            
            Referensi Memori (ZIP/Dokumen):
            $memoryContext
            
            Referensi GitHub (Real-time):
            $githubContext
            
            Jawablah dengan cerdas, gunakan referensi di atas jika relevan.
        """.trimIndent()

        try {
            // Simulasi Llama.cpp
            var mockResponse = "Halo! Saya PLA Agent (Topik: $activeTopic).\n"
            
            if (githubContext.isNotEmpty()) {
                mockResponse += "\nSaya sudah mengecek GitHub untuk Anda. Berikut referensi yang saya temukan:\n$githubContext"
            } else {
                mockResponse += "Saya siap membantu Anda."
            }

            for (char in mockResponse) {
                onToken(char.toString())
                Thread.sleep(30)
            }
        } catch (e: Exception) {
            onToken("\n[Error: ${e.message}]")
        }
    }

    fun isLoaded(): Boolean = isModelLoaded
}