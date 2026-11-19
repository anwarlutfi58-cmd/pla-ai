package com.pla.agent

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityTopicsBinding
import kotlinx.coroutines.launch

class TopicsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopicsBinding
    private lateinit var database: AppDatabase
    private var selectedVersion = ""
    private var listVersions = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = (application as AppContainer).database

        muatDaftarVersi()

        binding.btnSimpanTopik.setOnClickListener {
            if (selectedVersion.isEmpty()) {
                Toast.makeText(this, "Pilih versi dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                database.appDao().clearActiveVersion()
                database.appDao().setActiveVersion(selectedVersion)
                Toast.makeText(this@TopicsActivity, "Model aktif: $selectedVersion", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun muatDaftarVersi() {
        lifecycleScope.launch {
            val versions = database.appDao().getAllVersions()
            listVersions.clear()
            for (v in versions) {
                listVersions.add(v.versionName)
            }

            val adapter = ArrayAdapter(this@TopicsActivity, android.R.layout.simple_list_item_single_choice, listVersions)
            binding.listTopics.adapter = adapter
            binding.listTopics.choiceMode = ListView.CHOICE_MODE_SINGLE

            binding.listTopics.setOnItemClickListener { _, _, position, _ ->
                selectedVersion = listVersions[position]
            }

            if (listVersions.isNotEmpty()) {
                selectedVersion = listVersions[0]
            }
        }
    }
}