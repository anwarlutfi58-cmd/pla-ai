package com.pla.agent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityZipUploadBinding
import kotlinx.coroutines.launch

class ZipUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityZipUploadBinding
    private lateinit var zipReader: ZipReader
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityZipUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        zipReader = ZipReader(this)
        database = (application as AppContainer).database

        binding.btnPilihZip.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/zip"
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = data?.data
            if (fileUri != null) {
                binding.tvStatusZip.text = "Sedang membaca isi file..."
                
                lifecycleScope.launch {
                    val isiTeks = zipReader.bacaZip(fileUri)
                    
                    if (isiTeks.startsWith("Gagal") || isiTeks.startsWith("File")) {
                        binding.tvStatusZip.text = isiTeks
                    } else {
                        // Simpan ke Database
                        val activeTopic = database.appDao().getActiveTopic()?.name ?: "Umum"
                        database.appDao().insertMemory(
                            MemoryEntity(topic = activeTopic, content = isiTeks)
                        )
                        
                        binding.tvStatusZip.text = "Berhasil! AI telah mempelajari dokumen ini untuk topik $activeTopic."
                        Toast.makeText(this@ZipUploadActivity, "Materi disimpan ke Database!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}