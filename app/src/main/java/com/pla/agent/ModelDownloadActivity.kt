package com.pla.agent

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityModelDownloadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ModelDownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelDownloadBinding
    
    // Timeout 30 menit untuk file berukuran GB
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.MINUTES)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDownload.setOnClickListener {
            val selectedId = binding.rgModels.checkedRadioButtonId
            
            when (selectedId) {
                R.id.rbQwen -> {
                    val url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
                    mulaiUnduh(url, "qwen-1.5b.gguf")
                }
                R.id.rbLlama -> {
                    val url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                    mulaiUnduh(url, "llama-1b.gguf")
                }
                R.id.rbSmol -> {
                    val url = "https://huggingface.co/bartowski/SmolLM2-1.7B-Instruct-GGUF/resolve/main/SmolLM2-1.7B-Instruct-Q4_K_M.gguf"
                    mulaiUnduh(url, "smollm-1.7b.gguf")
                }
                else -> {
                    Toast.makeText(this, "Pilih model terlebih dahulu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mulaiUnduh(url: String, fileName: String) {
        binding.btnDownload.isEnabled = false
        binding.txtProgress.text = "Menyiapkan unduhan..."
        
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Server Error: ${response.code}")
                }

                val body = response.body ?: throw Exception("File tidak ditemukan")
                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(file)
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int

                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (contentLength > 0) {
                        val progress = (total * 100 / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            binding.txtProgress.text = "Mengunduh: $progress% (${total / (1024 * 1024)} MB)"
                        }
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    binding.txtProgress.text = "Unduhan Selesai!"
                    Toast.makeText(this@ModelDownloadActivity, "Model berhasil diunduh!", Toast.LENGTH_LONG).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtProgress.text = "Gagal: ${e.message}"
                    binding.btnDownload.isEnabled = true
                }
            }
        }
    }
}