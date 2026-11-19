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
    private var selectedTopic = "Umum"

    private val daftarTopik = arrayOf(
        "Umum", "Pemrograman & Coding (dari GitHub)", "Matematika", "Psikologi", 
        "Bahasa Asing", "Kesehatan & Medis", "Bisnis & Keuangan", "Sains & Teknologi", 
        "Sejarah & Budaya", "Desain & Seni"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = (application as AppContainer).database

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, daftarTopik)
        binding.listTopics.adapter = adapter
        binding.listTopics.choiceMode = ListView.CHOICE_MODE_SINGLE

        binding.listTopics.setOnItemClickListener { _, _, position, _ ->
            selectedTopic = daftarTopik[position]
        }

        binding.btnSimpanTopik.setOnClickListener {
            lifecycleScope.launch {
                database.appDao().clearActiveTopics()
                database.appDao().setActiveTopic(selectedTopic)
                Toast.makeText(this@TopicsActivity, "Topik diubah menjadi: $selectedTopic", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}