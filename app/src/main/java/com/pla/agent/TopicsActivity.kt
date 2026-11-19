package com.pla.agent

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivityTopicsBinding

class TopicsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopicsBinding
    private var selectedTopic = "Umum"

    // Ini adalah daftar topik yang bisa dipelajari AI
    private val daftarTopik = arrayOf(
        "Umum",
        "Pemrograman & Coding (dari GitHub)",
        "Matematika",
        "Psikologi",
        "Bahasa Asing",
        "Kesehatan & Medis",
        "Bisnis & Keuangan",
        "Sains & Teknologi",
        "Sejarah & Budaya",
        "Desain & Seni"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, daftarTopik)
        binding.listTopics.adapter = adapter
        binding.listTopics.choiceMode = ListView.CHOICE_MODE_SINGLE

        binding.listTopics.setOnItemClickListener { _, _, position, _ ->
            selectedTopic = daftarTopik[position]
        }

        binding.btnSimpanTopik.setOnClickListener {
            Toast.makeText(this, "Topik diubah menjadi: $selectedTopic", Toast.LENGTH_SHORT).show()
            // Nanti kita akan simpan topik ini ke database/SharedPreferences
            finish()
        }
    }
}