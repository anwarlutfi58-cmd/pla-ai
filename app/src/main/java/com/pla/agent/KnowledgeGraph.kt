package com.pla.agent

import android.content.Context
import java.io.File

class KnowledgeGraph(private val context: Context) {
    
    // Struktur graf sederhana: Node -> Map<Relasi, Set<NodeTujuan>>
    private val graph = HashMap<String, HashMap<String, MutableSet<String>>>()
    private var isBuilt = false
    
    companion object {
        private const val GRAPH_FILE = "knowledge_graph.txt"
        private const val MAX_EDGES_PER_NODE = 50 // Batas aman RAM 4GB
    }

    /**
     * Membangun graf pengetahuan dari dataset.
     * Menganalisis pola "A adalah B", "A bagian dari B", dll dari teks jawaban.
     */
    fun buildFromMemories(memories: List<String>) {
        if (isBuilt && memories.isEmpty()) return
        
        graph.clear()
        
        for (memory in memories) {
            val sentences = memory.lowercase().split(Regex("[.!?\\n]+"))
            for (sentence in sentences) {
                extractRelationships(sentence.trim())
            }
        }
        
        saveToDisk()
        isBuilt = true
    }

    /**
     * Ekstraksi relasi sederhana dari kalimat.
     * Pola: "[Kata1] [kata penghubung] [Kata2]"
     */
    private fun extractRelationships(sentence: String) {
        val words = sentence.split(Regex("\\s+")).filter { it.length > 2 }
        if (words.size < 3) return
        
        val connectors = setOf("adalah", "merupakan", "bagian", "dari", "dalam", "termasuk", 
                              "berasal", "terletak", "dikenal", "sebagai")
        
        for (i in 0 until words.size - 2) {
            if (connectors.contains(words[i + 1])) {
                val node1 = words[i]
                val relation = words[i + 1]
                val node2 = words[i + 2]
                
                addEdge(node1, relation, node2)
            }
        }
    }

    private fun addEdge(from: String, relation: String, to: String) {
        val edges = graph.getOrPut(from) { HashMap() }
        val targets = edges.getOrPut(relation) { mutableSetOf() }
        if (targets.size < MAX_EDGES_PER_NODE) {
            targets.add(to)
        }
    }

    /**
     * Mencari jalur hubungan antara dua konsep.
     * Contoh: query("jakarta", "indonesia") -> ["ibu_kota"]
     */
    fun findRelation(from: String, to: String): List<String> {
        val relations = mutableListOf<String>()
        val lowerFrom = from.lowercase()
        val lowerTo = to.lowercase()
        
        // Cari langsung
        graph[lowerFrom]?.forEach { (rel, targets) ->
            if (targets.any { it.lowercase() == lowerTo }) {
                relations.add(rel)
            }
        }
        
        // Cari terbalik (to -> from)
        graph[lowerTo]?.forEach { (rel, targets) ->
            if (targets.any { it.lowercase() == lowerFrom }) {
                relations.add("$rel (terbalik)")
            }
        }
        
        return relations
    }

    /**
     * Mendapatkan semua node yang terhubung dengan suatu konsep.
     */
    fun getConnectedNodes(concept: String): Map<String, Set<String>> {
        return graph[concept.lowercase()] ?: emptyMap()
    }

    private fun saveToDisk() {
        try {
            val file = File(context.filesDir, GRAPH_FILE)
            val sb = StringBuilder()
            for ((node, edges) in graph) {
                for ((rel, targets) in edges) {
                    sb.append(node).append("\t").append(rel).append("\t")
                        .append(targets.joinToString(",")).append("\n")
                }
            }
            file.writeText(sb.toString())
        } catch (e: Exception) { /* Ignore */ }
    }

    fun loadFromDisk(): Boolean {
        return try {
            val file = File(context.filesDir, GRAPH_FILE)
            if (!file.exists()) return false
            
            graph.clear()
            file.forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size == 3) {
                    val node = parts[0]
                    val rel = parts[1]
                    val targets = parts[2].split(",").toMutableSet()
                    graph.getOrPut(node) { HashMap() }[rel] = targets
                }
            }
            isBuilt = graph.isNotEmpty()
            isBuilt
        } catch (e: Exception) { false }
    }
}