package com.pla.agent

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
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
    private var currentVersions = mutableListOf<String>()
    private var selectedTrainingVersion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as AppContainer
        preferences = app.preferences
        database = app.database
        dataImporter = DataImporter(database)

        // Muat Status Switch (Pastikan ID ini ada di activity_settings.xml)
        binding.switchInternet.isChecked = preferences.isInternetEnabled()
        binding.switchTts.isChecked = preferences.isTtsEnabled()

        muatVersi()

        binding.btnBuatVersiBaru.setOnClickListener {
            tampilkanDialogBuatVersi()
        }

        binding.btnSyncKnowledge.setOnClickListener {
            if (selectedTrainingVersion.isEmpty()) {
                Toast.makeText(this, "Pilih versi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val url = binding.edtGithubRawUrl.text.toString().trim()
            if (url.isEmpty() || !url.endsWith(".json")) {
                Toast.makeText(this, "Masukkan link Raw GitHub berakhiran .json", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSyncKnowledge.isEnabled = false
            binding.tvSyncStatus.text = "Menarik data untuk $selectedTrainingVersion..."
            
            lifecycleScope.launch {
                dataImporter.importFromGitHub(url, selectedTrainingVersion) { status ->
                    runOnUiThread { binding.tvSyncStatus.text = status }
                }
                binding.edtGithubRawUrl.setText("")
                binding.btnSyncKnowledge.isEnabled = true
                muatDaftarSumber()
            }
        }

        binding.btnTopics.setOnClickListener {
            startActivity(Intent(this, TopicsActivity::class.java))
        }

        binding.btnBackSettings.setOnClickListener {
            finish()
        }

        binding.switchInternet.setOnCheckedChangeListener { _, isChecked ->
            preferences.setInternetEnabled(isChecked)
            if (isChecked) {
                LearningScheduler.startLearning(this)
            } else {
                LearningScheduler.stopLearning(this)
            }
        }

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            preferences.setTtsEnabled(isChecked)
        }
    }

    private fun muatVersi() {
        lifecycleScope.launch {
            var versions = database.appDao().getAllVersions()
            
            // Jika belum ada versi, buat V1 otomatis
            if (versions.isEmpty()) {
                database.appDao().insertVersion(VersionEntity(versionName = "V1. pla", isActiveForChat = true))
                versions = database.appDao().getAllVersions()
            }

            currentVersions.clear()
            for (v in versions) {
                currentVersions.add(v.versionName)
            }

            val adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, currentVersions)
            binding.spinnerVersion.adapter = adapter

            if (currentVersions.isNotEmpty()) {
                selectedTrainingVersion = currentVersions[0]
                muatDaftarSumber()
            }

            binding.spinnerVersion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    selectedTrainingVersion = currentVersions[position]
                    muatDaftarSumber()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun tampilkanDialogBuatVersi() {
        val input = EditText(this)
        input.hint = "Nama Versi (Misal: V2. pla)"
        input.setText("V${currentVersions.size + 1}. pla")

        AlertDialog.Builder(this)
            .setTitle("Buat Versi Model Baru")
            .setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val nama = input.text.toString().trim()
                if (nama.isNotEmpty()) {
                    lifecycleScope.launch {
                        database.appDao().insertVersion(VersionEntity(versionName = nama))
                        Toast.makeText(this@SettingsActivity, "Versi $nama berhasil dibuat!", Toast.LENGTH_SHORT).show()
                        muatVersi()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun muatDaftarSumber() {
        if (selectedTrainingVersion.isEmpty()) return
        lifecycleScope.launch {
            val sources = database.appDao().getSourcesByVersion(selectedTrainingVersion)
            val displaySources = if (sources.isEmpty()) {
                listOf("Belum ada link data untuk versi ini.")
            } else {
                sources
            }
            val adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_list_item_1, displaySources)
            binding.listSources.adapter = adapter
        }
    }
}