package com.pla.agent

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class ZipReader(private val context: Context) {

    // Membaca file ZIP dan menggabungkan semua teks (.txt, .md, .kt, .py, .js, .java)
    suspend fun bacaZip(uri: Uri): String = withContext(Dispatchers.IO) {
        val hasilTeks = StringBuilder()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                // Hanya baca file teks/kode, abaikan gambar atau file biner
                if (entry.name.endsWith(".txt") || entry.name.endsWith(".md") || 
                    entry.name.endsWith(".kt") || entry.name.endsWith(".py") || 
                    entry.name.endsWith(".js") || entry.name.endsWith(".java")) {
                    
                    hasilTeks.append("\n--- FILE: ${entry.name} ---\n")
                    val reader = BufferedReader(InputStreamReader(zipInputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        hasilTeks.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            
            if (hasilTeks.isEmpty()) {
                return@withContext "File ZIP kosong atau tidak ada file teks di dalamnya."
            }
            
            // Batasi panjang teks maksimal 10.000 karakter agar RAM tidak penuh
            return@withContext hasilTeks.toString().take(10000)

        } catch (e: Exception) {
            return@withContext "Gagal membaca ZIP: ${e.message}"
        }
    }
}