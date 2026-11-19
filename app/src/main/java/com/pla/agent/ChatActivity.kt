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
    private lateinit var preferences: AppPreferences
    private val chatHistory = StringBuilder()
    private var isGenerating = false
    private var currentVersion = "V1. pla"
    private var currentAiName = "PLA Agent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as AppContainer
        aiEngine = app.aiEngine
        database = app.database
        voiceEngine = VoiceEngine(this)
        preferences = app.preferences

        currentAiName = preferences.getAiName()
        binding.imgAvatar.setImageResource(preferences.getAiAvatar())
        
        // Sembunyikan tombol ganti model karena kita murni pakai Independent AI
        binding.btnGantiModel.visibility = android.view.View.GONE
        binding.tvModelInfo.text = "Independent AI Engine"

        lifecycleScope.launch {
            currentVersion = database.appDao().getActiveVersion()?.versionName ?: "V1. pla"
            updateTopicInfo(currentVersion)
            muatRiwayatChat()
        }

        binding.btnSend.setOnClickListener {
            val input = binding.edtMessage.text.toString().trim()
            if (input.isNotEmpty() && !isGenerating) {
                binding.edtMessage.setText("")
                kirimPesan(input)
            }
        }
    }

    private fun muatRiwayatChat() {
        lifecycleScope.launch {
            val history = database.appDao().getAllChats()
            chatHistory.clear()
            for (chat in history) {
                chatHistory.append("${chat.sender}: ${chat.message}\n\n")
            }
            binding.tvChatHistory.text = chatHistory.toString()
            scrollKeBawah()
        }
    }

    private fun updateTopicInfo(versionName: String) {
        binding.tvTopicInfo.text = "Model: $versionName"
        if (preferences.getAiAvatar() == R.drawable.avatar_umum) {
            when {
                versionName.contains("Coding", ignoreCase = true) -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_coding)
                }
                versionName.contains("Kesehatan", ignoreCase = true) || versionName.contains("Psikologi", ignoreCase = true) -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_dokter)
                }
                else -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_umum)
                }
            }
        }
    }

    private fun kirimPesan(userInput: String) {
        isGenerating = true
        binding.btnSend.isEnabled = false

        chatHistory.append("User: $userInput\n\n")
        binding.tvChatHistory.text = chatHistory.toString()
        scrollKeBawah()

        lifecycleScope.launch {
            database.appDao().insertChat(ChatHistoryEntity(sender = "User", message = userInput))
        }

        chatHistory.append("$currentAiName: ")
        binding.tvChatHistory.text = chatHistory.toString()

        val aiResponse = StringBuilder()

        lifecycleScope.launch {
            aiEngine.generateResponse(
                userInput = userInput,
                onStatusUpdate = { status ->
                    runOnUiThread { binding.tvModelInfo.text = status }
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
                binding.tvModelInfo.text = "Independent AI Engine"
                
                lifecycleScope.launch {
                    database.appDao().insertChat(ChatHistoryEntity(sender = currentAiName, message = aiResponse.toString()))
                }

                val isTtsOn = preferences.isTtsEnabled()
                voiceEngine.speak(aiResponse.toString(), currentVersion, isTtsOn)
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