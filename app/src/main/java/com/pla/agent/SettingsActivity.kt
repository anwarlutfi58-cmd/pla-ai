package com.pla.agent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: AppPreferences

    // Daftar gambar avatar yang tersedia
    private val avatarList = listOf(
        R.drawable.avatar_umum,
        R.drawable.avatar_coding,
        R.drawable.avatar_dokter
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = (application as AppContainer).preferences

        // Muat Pengaturan Karakter
        binding.edtAiName.setText(preferences.getAiName())
        
        val avatarNames = resources.getStringArray(R.array.avatar_names)
        val adapterAvatar = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, avatarNames)
        binding.spinnerAvatar.adapter = adapterAvatar
        
        // Set spinner ke avatar yang sedang aktif
        val currentAvatar = preferences.getAiAvatar()
        val avatarIndex = avatarList.indexOf(currentAvatar)
        if (avatarIndex != -1) {
            binding.spinnerAvatar.setSelection(avatarIndex)
        }

        binding.btnSimpanKarakter.setOnClickListener {
            val newName = binding.edtAiName.text.toString().trim()
            val selectedAvatarIndex = binding.spinnerAvatar.selectedItemPosition
            
            if (newName.isNotEmpty()) {
                preferences.setAiName(newName)
                preferences.setAiAvatar(avatarList[selectedAvatarIndex])
                Toast.makeText(this, "Karakter berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nama AI tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        // Muat Status Switch Internet
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