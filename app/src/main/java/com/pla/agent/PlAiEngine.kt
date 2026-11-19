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

    // Fungsi baru: Mencari semua file .gguf di folder Download
    fun getAvailableModels(): List<File> {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.listFiles { file -> file.extension == "gguf" }?.toList() ?: emptyList()
    }

    // Fungsi baru: Membongkar model dari RAM sebelum ganti model
    fun unloadModel() {
        isModelLoaded = false
        currentModelName = ""
        // Di Llama.cpp asli, di sini kita panggil fungsi free/close
    }

    suspend fun loadModel(modelFile: File, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        unloadModel() // Bersihkan RAM dulu
        
        onProgress("Memuat ${modelFile.name}...")
        try {
            Thread.sleep(1500) // Simulasi loading C++
            isModelLoaded = true
            currentModelName = modelFile.name
            onProgress("Model siap: ${modelFile.name}")
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
        
        val isCodingQuery = activeTopic.contains("Coding", ignoreCase = true) || 
                            userInput.contains("kode", ignoreCase = true) || 
                            userInput.contains("coding", ignoreCase = true) ||
                            userInput.contains("github", ignoreCase = true)

        if (isCodingQuery) {
            onStatusUpdate("Mencari referensi di GitHub...")
            githubContext = githubScraper.cariReferensi(userInput)
        }

        val memoryContext = memories.joinToString("\n") { "Dokumen: ${it.content.take(500)}" }

        val systemPrompt = """
            Anda adalah PLA Agent.
            Topik saat ini: $activeTopic.
            Referensi Memori: $memoryContext
            Referensi GitHub: $githubContext
        """.trimIndent()

        try {
            var mockResponse = "Halo! Saya PLA Agent (Model: $currentModelName, Topik: $activeTopic).\n"
            if (githubContext.isNotEmpty()) {
                mockResponse += "\nReferensi GitHub:\n$githubContext"
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