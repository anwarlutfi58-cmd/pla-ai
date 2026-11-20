package com.pla.agent

import android.content.Context
import java.io.File

data class MemoryEntry(
    val tokens: List<String>,
    val intent: UserIntent,
    val timestamp: Long = System.currentTimeMillis(),
    var weight: Double = 1.0 // Bobot ingatan, akan menurun seiring waktu
)

class ContextMemory(private val context: Context) {
    
    private val history = mutableListOf<MemoryEntry>()
    private val maxEntries = 5 // Simpan 5 interaksi terakhir (aman untuk RAM 4GB)
    
    companion object {
        private const val MEMORY_FILE = "context_memory.txt"
        private const val DECAY_FACTOR = 0.7 // Ingatan lama berkurang 30% tiap interaksi baru
    }

    /**
     * Menambahkan interaksi baru ke ingatan.
     * Otomatis menerapkan decay pada ingatan lama dan membuang yang paling tua jika penuh.
     */
    fun add(tokens: List<String>, intent: UserIntent) {
        // Terapkan decay pada semua entri yang ada
        for (entry in history) {
            entry.weight *= DECAY_FACTOR
        }
        
        // Tambahkan entri baru dengan bobot penuh
        history.add(MemoryEntry(tokens, intent))
        
        // Buang entri terlama jika melebihi batas
        if (history.size > maxEntries) {
            history.removeAt(0)
        }
        
        saveToDisk()
    }

    /**
     * Mendapatkan token gabungan dari ingatan yang masih relevan (bobot > threshold).
     * Digunakan untuk memperluas query saat user bertanya lanjutan.
     */
    fun getRelevantTokens(threshold: Double = 0.2): List<String> {
        return history.filter { it.weight >= threshold }
            .flatMap { it.tokens }
            .distinct()
    }

    /**
     * Mendapatkan intent dari interaksi terakhir.
     */
    fun getLastIntent(): UserIntent? {
        return history.lastOrNull()?.intent
    }

    /**
     * Mengecek apakah ini kemungkinan pertanyaan lanjutan.
     */
    fun isLikelyContinuation(currentIntent: UserIntent): Boolean {
        return currentIntent == UserIntent.CONTINUATION || 
               (history.isNotEmpty() && history.last().weight > 0.5)
    }

    /**
     * Membersihkan ingatan (misal saat versi data berubah).
     */
    fun clear() {
        history.clear()
        saveToDisk()
    }

    private fun saveToDisk() {
        try {
            val file = File(context.filesDir, MEMORY_FILE)
            val sb = StringBuilder()
            for (entry in history) {
                sb.append(entry.tokens.joinToString(",")).append("\t")
                  .append(entry.intent.name).append("\t")
                  .append(entry.weight).append("\n")
            }
            file.writeText(sb.toString())
        } catch (e: Exception) { /* Ignore */ }
    }

    fun loadFromDisk(): Boolean {
        return try {
            val file = File(context.filesDir, MEMORY_FILE)
            if (!file.exists()) return false
            
            history.clear()
            file.forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size == 3) {
                    val tokens = parts[0].split(",").filter { it.isNotBlank() }
                    val intent = UserIntent.valueOf(parts[1])
                    val weight = parts[2].toDoubleOrNull() ?: 1.0
                    history.add(MemoryEntry(tokens, intent, System.currentTimeMillis(), weight))
                }
            }
            true
        } catch (e: Exception) { false }
    }
}