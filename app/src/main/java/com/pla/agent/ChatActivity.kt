package com.pla.agent

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var aiEngine: PlAiEngine
    private val chatHistory = StringBuilder()
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AMBIL MESIN AI DARI GUDANG GLOBAL (AppContainer)
        // Agar memori dari file ZIP yang diunggah tadi langsung terbaca di sini
        aiEngine = (application as AppContainer).aiEngine

        // Jika model belum dimuat, muat sekarang
        if (!aiEngine.isLoaded()) {
            muatModel()
        } else {
            binding.tvModelInfo.text = "Model siap!"
        }

        binding.btnSend.setOnClickListener {
            val input = binding.edtMessage.text.toString().trim()
            if (input.isNotEmpty() && !isGenerating) {
                binding.edtMessage.setText("")
                kirimPesan(input)
            }
        }
    }

    private fun muatModel() {
        lifecycleScope.launch {
            val success = aiEngine.loadModel { progress ->
                runOnUiThread {
                    binding.tvModelInfo.text = progress
                }
            }
            if (!success) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Gagal memuat. Unduh model di Pengaturan!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun kirimPesan(userInput: String) {
        isGenerating = true
        binding.btnSend.isEnabled = false

        chatHistory.append("Anda: $userInput\n\n")
        binding.tvChatHistory.text = chatHistory.toString()
        scrollKeBawah()

        chatHistory.append("PLA Agent: ")
        binding.tvChatHistory.text = chatHistory.toString()

        lifecycleScope.launch {
            // Topic "Umum" bisa kita ubah nanti agar membaca dari database Topik
            aiEngine.generateResponse(userInput, topic = "Umum") { token ->
                runOnUiThread {
                    chatHistory.append(token)
                    binding.tvChatHistory.text = chatHistory.toString()
                    scrollKeBawah()
                }
            }
            runOnUiThread {
                chatHistory.append("\n\n")
                isGenerating = false
                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun scrollKeBawah() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
}