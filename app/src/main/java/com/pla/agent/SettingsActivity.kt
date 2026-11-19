package com.pla.agent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = (application as AppContainer).preferences

        // Memuat status switch terakhir
        binding.switchInternet.isChecked = preferences.isInternetEnabled()

        binding.btnManageModels.setOnClickListener {
            startActivity(Intent(this, ModelDownloadActivity::class.java))
        }

        binding.btnTopics.setOnClickListener {
            startActivity(Intent(this, TopicsActivity::class.java))
        }

        binding.btnUploadZip.setOnClickListener {
            startActivity(Intent(this, ZipUploadActivity::class.java))
        }

        binding.btnBackSettings.setOnClickListener {
            finish()
        }

        binding.btnStartJarvis.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Izinkan aplikasi ini untuk muncul di atas aplikasi lain", Toast.LENGTH_LONG).show()
            } else {
                startService(Intent(this, FloatingService::class.java))
                Toast.makeText(this, "Jarvis (Melayang) Diaktifkan!", Toast.LENGTH_SHORT).show()
                finish() 
            }
        }

        binding.switchInternet.setOnCheckedChangeListener { _, isChecked ->
            preferences.setInternetEnabled(isChecked)
            if (isChecked) {
                LearningScheduler.startLearning(this)
                Toast.makeText(this, "AI akan belajar diam-diam saat HP tidak dipakai.", Toast.LENGTH_LONG).show()
            } else {
                LearningScheduler.stopLearning(this)
                Toast.makeText(this, "AI berhenti belajar mandiri.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}