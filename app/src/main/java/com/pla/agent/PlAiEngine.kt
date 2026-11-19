package com.pla.agent

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlAiEngine(private val context: Context) {

    // Nanti kita akan mengganti variabel ini dengan library Llama.cpp asli.
    // Untuk saat ini, ini adalah kerangka logikanya.
    private var isModelLoaded = false
    private var currentModelName = ""

    // Fungsi untuk mencari model .gguf yang ada di folder Download aplikasi
    fun getAvailableModel(): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.listFiles()?.firstOrNull { it.name.endsWith(".gguf") }
    }

    // Fungsi untuk memuat model ke dalam RAM (Llama.cpp)
    suspend fun loadModel(onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val modelFile = getAvailableModel()
        if (modelFile == null) {
            onProgress("Model tidak ditemukan. Silakan unduh di Pengaturan.")
            return@withContext false
        }

        onProgress("Memuat model ${modelFile.name} ke dalam RAM...")
        
        try {
            // DI SINILAH KODE LLAMA.CPP DIPANGGIL
            // Contoh (pseudo-code):
            // LlamaModel.load(modelFile.absolutePath)
            
            Thread.sleep(2000) // Simulasi proses loading
            isModelLoaded = true
            currentModelName = modelFile.name
            onProgress("Model berhasil dimuat!")
            return@withContext true
        } catch (e: Exception) {
            onProgress("Gagal memuat model: ${e.message}")
            return@withContext false
        }
    }

    // Fungsi untuk menghasilkan jawaban dari AI
    suspend fun generateResponse(
        userInput: String, 
        topic: String = "Umum",
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            onToken("Error: Model belum dimuat.")
            return@withContext
        }

        // SYSTEM PROMPT: Ini yang membuat AI punya karakter/evolusi
        val systemPrompt = """
            Anda adalah PLA Agent, asisten AI profesional.
            Saat ini Anda sedang mendalami topik: $topic.
            Jika topiknya Coding, jawablah dengan tegas dan teknis.
            Jika topiknya Psikologi, jawablah dengan tenang dan empatik.
            Selalu jawab dalam bahasa Indonesia yang baik dan benar.
        """.trimIndent()

        val fullPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userInput<|im_end|>\n<|im_start|>assistant\n"

        try {
            // DI SINILAH PROSES GENERATE LLAMA.CPP DIPANGGIL
            // Contoh (pseudo-code):
            // LlamaModel.generate(fullPrompt) { token -> onToken(token) }
            
            // Simulasi jawaban AI (karena library C++ belum terpasang sempurna)
            val mockResponse = "Halo! Saya PLA Agent. Saya siap membantu Anda mempelajari topik $topic. (Ini adalah simulasi jawaban karena mesin Llama.cpp sedang dalam tahap integrasi)."
            
            // Simulasi mengetik huruf per huruf
            for (char in mockResponse) {
                onToken(char.toString())
                Thread.sleep(30) // Jeda 30ms per huruf (seperti manusia mengetik)
            }
            
        } catch (e: Exception) {
            onToken("\n[Error: ${e.message}]")
        }
    }

    fun isLoaded(): Boolean = isModelLoaded
}