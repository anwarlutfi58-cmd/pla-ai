package com.pla.agent

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = (application as AppContainer).database

        muatRiwayat()

        binding.btnHapusRiwayat.setOnClickListener {
            lifecycleScope.launch {
                database.appDao().clearAllChats()
                Toast.makeText(this@HistoryActivity, "Riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
                muatRiwayat()
            }
        }

        binding.btnBackHistory.setOnClickListener {
            finish()
        }
    }

    private fun muatRiwayat() {
        lifecycleScope.launch {
            val history = database.appDao().getAllChats()
            
            if (history.isEmpty()) {
                val listKosong = arrayListOf("Belum ada riwayat percakapan.")
                val adapter = ArrayAdapter(this@HistoryActivity, android.R.layout.simple_list_item_1, listKosong)
                binding.listHistory.adapter = adapter
            } else {
                val formatTanggal = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                val listString = history.map { 
                    val tanggal = formatTanggal.format(Date(it.timestamp))
                    "[$tanggal] ${it.sender}:\n${it.message}"
                }
                
                val adapter = ArrayAdapter(this@HistoryActivity, android.R.layout.simple_list_item_1, listString)
                binding.listHistory.adapter = adapter
            }
        }
    }
}