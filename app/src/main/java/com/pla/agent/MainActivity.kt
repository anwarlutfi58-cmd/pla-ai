package com.pla.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat tampilan: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnChat.setOnClickListener {
            try {
                startActivity(Intent(this, ChatActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error Chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHistory.setOnClickListener {
            try {
                startActivity(Intent(this, HistoryActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error Riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSettings.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error Pengaturan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}