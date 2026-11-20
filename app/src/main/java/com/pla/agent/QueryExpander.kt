package com.pla.agent

import android.content.Context
import java.io.File

class QueryExpander(private val context: Context) {
    
    private val synonymMap = HashMap<String, MutableSet<String>>()
    var isBuilt = false // UBAH DARI PRIVATE KE PUBLIC
    
    companion object {
        private const val SYNONYM_FILE = "synonym_cache.txt"
        private const val MAX_SYNONYMS_PER_WORD = 8
    }

    fun buildFromMemories(memories: List<String>) {
        if (isBuilt && memories.isEmpty()) return
        
        synonymMap.clear()
        val wordCoOccurrence = HashMap<String, HashMap<String, Int>>()
        
        for (memory in memories) {
            val words = memory.lowercase()
                .replace(Regex("[^a-z\\s]"), "")
                .split(Regex("\\s+"))
                .filter { it.length > 3 }
            
            for (i in words.indices) {
                for (j in i + 1 until words.size) {
                    if (words[i] != words[j]) {
                        wordCoOccurrence.getOrPut(words[i]) { HashMap() }
                            .merge(words[j], 1, Int::plus)
                        wordCoOccurrence.getOrPut(words[j]) { HashMap() }
                            .merge(words[i], 1, Int::plus)
                    }
                }
            }
        }
        
        for ((word, related) in wordCoOccurrence) {
            val sorted = related.entries.sortedByDescending { it.value }
                .take(MAX_SYNONYMS_PER_WORD)
                .map { it.key }
            if (sorted.isNotEmpty()) {
                synonymMap[word] = sorted.toMutableSet()
            }
        }
        
        saveToDisk()
        isBuilt = true
    }

    fun expandQuery(tokens: List<String>): Set<String> {
        val expanded = tokens.toMutableSet()
        for (token in tokens) {
            synonymMap[token]?.let { synonyms ->
                expanded.addAll(synonyms)
            }
        }
        return expanded
    }

    fun addManualSynonym(word: String, synonym: String) {
        synonymMap.getOrPut(word.lowercase()) { mutableSetOf() }.add(synonym.lowercase())
        synonymMap.getOrPut(synonym.lowercase()) { mutableSetOf() }.add(word.lowercase())
        saveToDisk()
    }

    private fun saveToDisk() {
        try {
            val file = File(context.filesDir, SYNONYM_FILE)
            val sb = StringBuilder()
            for ((word, syns) in synonymMap) {
                sb.append(word).append("\t").append(syns.joinToString(",")).append("\n")
            }
            file.writeText(sb.toString())
        } catch (e: Exception) { /* Ignore */ }
    }

    fun loadFromDisk(): Boolean {
        return try {
            val file = File(context.filesDir, SYNONYM_FILE)
            if (!file.exists()) return false
            
            synonymMap.clear()
            file.forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size == 2) {
                    val word = parts[0]
                    val syns = parts[1].split(",").toMutableSet()
                    synonymMap[word] = syns
                }
            }
            isBuilt = !synonymMap.isEmpty()
            isBuilt
        } catch (e: Exception) { false }
    }
}