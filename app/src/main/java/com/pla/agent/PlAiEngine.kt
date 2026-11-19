package com.pla.agent

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlAiEngine(
    private val context: Context,
    private val appDao: AppDao // Tambahkan DAO Database
) {
    private var isModelLoaded = false
    private var currentModelName = ""

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
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            onToken("Error: Model belum dimuat.")
            return@withContext
        }

        // 1. Ambil Topik yang sedang aktif dari Database
        val activeTopic = appDao.getActiveTopic()?.name ?: "Umum"

        // 2. Ambil Memori Belajar dari Database
        val memories = appDao.getMemoriesByTopic(activeTopic)
        val memoryContext = if (memories.isNotEmpty()) {
            memories.joinToString("\n") { "Dokumen: ${it.content.take(500)}" }
        } else {
            "Tidak ada dokumen referensi."
        }

        val systemPrompt = """
            Anda adalah PLA Agent.
            Topik saat ini: $activeTopic.
            Gunakan pengetahuan dari dokumen referensi berikut untuk menjawab:
            $memoryContext
            
            Jawablah dengan cerdas dan profesional.
        """.trimIndent()

        try {
            // Simulasi Llama.cpp (Nanti diganti kode C++)
            val mockResponse = "Halo! Saya PLA Agent. (Topik: $activeTopic). Saya membaca ${memories.size} memori dari database. Siap membantu!"
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