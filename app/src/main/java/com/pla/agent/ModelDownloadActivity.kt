package com.pla.agent

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.ArrayAdapter
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
    private lateinit var aiEngine: PlAiEngine
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.MINUTES)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aiEngine = (application as AppContainer).aiEngine

        cekSisaMemori()
        refreshDaftarModel()

        binding.btnHapusModel.setOnClickListener {
            hapusModelTerpilih()
        }

        binding.btnDownload.setOnClickListener {
            val selectedId = binding.rgModels.checkedRadioButtonId
            when (selectedId) {
                R.id.rbQwen -> mulaiUnduh("https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf", "qwen-1.5b.gguf")
                R.id.rbLlama -> mulaiUnduh("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf", "llama-1b.gguf")
                R.id.rbSmol -> mulaiUnduh("https://huggingface.co/bartowski/SmolLM2-1.7B-Instruct-GGUF/resolve/main/SmolLM2-1.7B-Instruct-Q4_K_M.gguf", "smollm-1.7b.gguf")
                else -> Toast.makeText(this, "Pilih model terlebih dahulu!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cekSisaMemori() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val gbAvailable = bytesAvailable / (1024f * 1024f * 1024f)
            binding.tvStorageInfo.text = String.format("Sisa Memori HP: %.2f GB", gbAvailable)
        } catch (e: Exception) {
            binding.tvStorageInfo.text = "Gagal mengecek memori."
        }
    }

    private fun refreshDaftarModel() {
        val models = aiEngine.getAvailableModels()
        val namaModel = if (models.isEmpty()) {
            arrayListOf("Belum ada model terunduh")
        } else {
            models.map { it.name }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, namaModel)
        binding.spinnerModels.adapter = adapter
    }

    private fun hapusModelTerpilih() {
        val models = aiEngine.getAvailableModels()
        if (models.isEmpty()) {
            Toast.makeText(this, "Tidak ada model untuk dihapus.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPosition = binding.spinnerModels.selectedItemPosition
        val fileTerpilih = models[selectedPosition]

        if (fileTerpilih.delete()) {
            Toast.makeText(this, "Model ${fileTerpilih.name} berhasil dihapus.", Toast.LENGTH_SHORT).show()
            refreshDaftarModel()
            cekSisaMemori()
        } else {
            Toast.makeText(this, "Gagal menghapus model.", Toast.LENGTH_SHORT).show()
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
                    binding.btnDownload.isEnabled = true
                    refreshDaftarModel()
                    cekSisaMemori()
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