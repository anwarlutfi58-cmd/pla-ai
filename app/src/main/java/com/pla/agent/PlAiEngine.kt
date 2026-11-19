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

    fun getAvailableModels(): List<File> {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.listFiles { file -> file.extension == "gguf" }?.toList() ?: emptyList()
    }

    fun unloadModel() {
        isModelLoaded = false
        currentModelName = ""
    }

    suspend fun loadModel(modelFile: File, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        unloadModel()
        onProgress("Memuat ${modelFile.name}...")
        try {
            Thread.sleep(1500)
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
        
        // BELAJAR DIAM-DIAM: Ambil 10 chat terakhir untuk memahami konteks user
        val recentChats = appDao.getRecentChats().reversed()
        val chatContext = if (recentChats.isNotEmpty()) {
            recentChats.joinToString("\n") { "${it.sender}: ${it.message}" }
        } else {
            "Belum ada riwayat percakapan."
        }

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

        // SYSTEM PROMPT DIPERBARUI: Menggunakan Riwayat Chat agar AI paham kebiasaan user
        val systemPrompt = """
            Anda adalah PLA Agent.
            Topik saat ini: $activeTopic.
            
            Riwayat Percakapan Terakhir (Gunakan ini untuk memahami gaya bicara dan keinginan pengguna):
            $chatContext
            
            Referensi Memori (ZIP/Dokumen):
            $memoryContext
            
            Referensi GitHub (Real-time):
            $githubContext
            
            Jawablah dengan cerdas dan sesuaikan dengan kebiasaan pengguna.
        """.trimIndent()

        try {
            var mockResponse = "Halo! Saya PLA Agent (Model: $currentModelName, Topik: $activeTopic).\n"
            
            if (recentChats.isNotEmpty()) {
                mockResponse += "Saya sudah mempelajari riwayat obrolan kita sebelumnya (Total ${recentChats.size} pesan terakhir).\n"
            }

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