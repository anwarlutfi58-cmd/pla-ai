package com.pla.agent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: AppPreferences
    private lateinit var database: AppDatabase
    private lateinit var dataImporter: DataImporter

    private val avatarList = listOf(
        R.drawable.avatar_umum,
        R.drawable.avatar_coding,
        R.drawable.avatar_dokter
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pengaman: Cek apakah AppContainer siap sebelum mengaksesnya
        val app = application as? AppContainer
        if (app == null) {
            Toast.makeText(this, "Gagal memuat konfigurasi aplikasi.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = app.preferences
        database = app.database
        dataImporter = DataImporter(database)

        // Muat Pengaturan Karakter
        binding.edtAiName.setText(preferences.getAiName())
        
        val avatarNames = resources.getStringArray(R.array.avatar_names)
        val adapterAvatar = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, avatarNames)
        binding.spinnerAvatar.adapter = adapterAvatar
        
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

        // LOGIKA SINKRONISASI GITHUB (INI YANG MENGHUBUNGKAN LINK KAMU)
        binding.btnSyncKnowledge.setOnClickListener {
            val url = binding.edtGithubRawUrl.text.toString().trim()
            if (url.isEmpty() || !url.endsWith(".json")) {
                Toast.makeText(this, "Masukkan link Raw GitHub berakhiran .json", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnSyncKnowledge.isEnabled = false
                binding.tvSyncStatus.text = "Memulai sinkronisasi..."
                
                lifecycleScope.launch {
                    val currentTopic = database.appDao().getActiveTopic()?.name ?: "Umum"
                    dataImporter.importFromGitHub(url, currentTopic) { status ->
                        runOnUiThread {
                            binding.tvSyncStatus.text = status
                        }
                    }
                    binding.btnSyncKnowledge.isEnabled = true
                }
            }
        }

        // Muat Status Switch
        binding.switchInternet.isChecked = preferences.isInternetEnabled()
        binding.switchTts.isChecked = preferences.isTtsEnabled()

        binding.btnIzinAksesibilitas.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Cari 'PLA Agent' dan aktifkan izinnya.", Toast.LENGTH_LONG).show()
        }

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
                val serviceIntent = Intent(this, JarvisBackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "Jarvis Aktif di Latar Belakang!", Toast.LENGTH_SHORT).show()
                finish() 
            }
        }

        binding.btnStopJarvis.setOnClickListener {
            stopService(Intent(this, JarvisBackgroundService::class.java))
            stopService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Jarvis Dimatikan.", Toast.LENGTH_SHORT).show()
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

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            preferences.setTtsEnabled(isChecked)
        }
    }
}