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
    private lateinit var database: AppDatabase
    private lateinit var voiceEngine: VoiceEngine
    private val chatHistory = StringBuilder()
    private var isGenerating = false
    private var currentTopic = "Umum"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as AppContainer
        aiEngine = app.aiEngine
        database = app.database
        voiceEngine = VoiceEngine(this)

        lifecycleScope.launch {
            currentTopic = database.appDao().getActiveTopic()?.name ?: "Umum"
            updateAvatarAndTopic(currentTopic)
        }

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

    private fun updateAvatarAndTopic(topic: String) {
        binding.tvTopicInfo.text = "Topik: $topic"
        
        when {
            topic.contains("Coding", ignoreCase = true) -> {
                binding.imgAvatar.setImageResource(R.drawable.avatar_coding)
            }
            topic.contains("Kesehatan", ignoreCase = true) || topic.contains("Psikologi", ignoreCase = true) -> {
                binding.imgAvatar.setImageResource(R.drawable.avatar_dokter)
            }
            else -> {
                binding.imgAvatar.setImageResource(R.drawable.avatar_umum)
            }
        }
    }

    private fun muatModel() {
        lifecycleScope.launch {
            val success = aiEngine.loadModel { progress ->
                runOnUiThread { binding.tvModelInfo.text = progress }
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

        val aiResponse = StringBuilder()

        lifecycleScope.launch {
            aiEngine.generateResponse(
                userInput = userInput,
                onStatusUpdate = { status ->
                    runOnUiThread {
                        // Menampilkan status (misal: "Mencari referensi di GitHub...")
                        binding.tvModelInfo.text = status
                    }
                },
                onToken = { token ->
                    runOnUiThread {
                        aiResponse.append(token)
                        chatHistory.append(token)
                        binding.tvChatHistory.text = chatHistory.toString()
                        scrollKeBawah()
                    }
                }
            )
            
            runOnUiThread {
                chatHistory.append("\n\n")
                isGenerating = false
                binding.btnSend.isEnabled = true
                binding.tvModelInfo.text = "Model siap!"
                
                voiceEngine.speak(aiResponse.toString(), currentTopic)
            }
        }
    }

    private fun scrollKeBawah() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.stop()
        voiceEngine.shutdown()
    }
}