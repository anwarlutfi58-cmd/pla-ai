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

        aiEngine = PlAiEngine(this)

        // Memuat model saat layar dibuka
        muatModel()

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

        // Tampilkan pesan user
        chatHistory.append("Anda: $userInput\n\n")
        binding.tvChatHistory.text = chatHistory.toString()
        scrollKeBawah()

        // Tampilkan placeholder AI
        chatHistory.append("PLA Agent: ")
        binding.tvChatHistory.text = chatHistory.toString()

        lifecycleScope.launch {
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