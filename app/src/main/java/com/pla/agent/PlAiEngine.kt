package com.pla.agent

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlAiEngine(private val context: Context) {

    private var isModelLoaded = false
    private var currentModelName = ""
    
    // VARIABEL MEMORI: Menyimpan hasil belajar dari ZIP / GitHub
    private var memoriBelajar: String = ""

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

    // FUNGSI BARU: Untuk memasukkan ilmu dari ZIP atau GitHub ke dalam otak AI
    fun simpanMemoriBelajar(teksBaru: String) {
        // Gabungkan memori lama dengan yang baru
        memoriBelajar += "\n$teksBaru\n"
    }

    suspend fun generateResponse(
        userInput: String, 
        topic: String = "Umum",
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            onToken("Error: Model belum dimuat.")
            return@withContext
        }

        // AI akan membaca memori belajarnya sebelum menjawab
        val systemPrompt = """
            Anda adalah PLA Agent.
            Topik saat ini: $topic.
            Gunakan pengetahuan dari dokumen referensi berikut untuk menjawab:
            $memoriBelajar
            
            Jawablah dengan cerdas dan profesional.
        """.trimIndent()

        val fullPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userInput<|im_end|>\n<|im_start|>assistant\n"

        try {
            val mockResponse = "Halo! Saya PLA Agent. Saya sudah membaca referensi Anda (Total memori: ${memoriBelajar.length} karakter). Saya siap menjawab pertanyaan Anda tentang $topic."
            
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