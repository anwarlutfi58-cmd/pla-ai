package com.pla.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnManageModels.setOnClickListener {
            startActivity(Intent(this, ModelDownloadActivity::class.java))
        }

        binding.btnBackSettings.setOnClickListener {
            finish()
        }

        binding.switchInternet.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Akses internet AI diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Akses internet AI dimatikan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}