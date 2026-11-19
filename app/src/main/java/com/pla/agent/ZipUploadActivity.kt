package com.pla.agent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivityZipUploadBinding

class ZipUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityZipUploadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityZipUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                binding.tvStatusZip.text = "File terpilih: ${fileUri.lastPathSegment}"
                Toast.makeText(this, "File ZIP berhasil dipilih. AI akan membaca isinya.", Toast.LENGTH_LONG).show()
                
                // Di sini nanti kita akan panggil ZipReader.kt untuk membongkar isi file
                // dan memasukkannya ke memori AI (Database).
            }
        }
    }
}