package com.pla.agent

import kotlin.math.sqrt

data class RankedMemory(
    val content: String,
    val score: Double,
    val tokens: Set<String>
)

object ResponseReranker {

    /**
     * Melakukan reranking pada kandidat jawaban untuk memaksimalkan keberagaman (diversity).
     * Menggunakan algoritma MMR (Maximal Marginal Relevance) sederhana.
     * @param candidates Daftar kandidat yang sudah diurutkan berdasarkan skor BM25
     * @param lambda Bobot relevansi vs diversitas (0.7 = lebih utamakan relevansi)
     * @param topK Jumlah hasil akhir yang diinginkan
     */
    fun rerank(candidates: List<RankedMemory>, lambda: Double = 0.7, topK: Int = 3): List<RankedMemory> {
        if (candidates.size <= topK) return candidates
        
        val selected = mutableListOf<RankedMemory>()
        val remaining = candidates.toMutableList()
        
        // Ambil kandidat terbaik pertama sebagai anchor
        val first = remaining.removeAt(0)
        selected.add(first)
        
        while (selected.size < topK && remaining.isNotEmpty()) {
            var bestIdx = -1
            var bestScore = -Double.MAX_VALUE
            
            for ((idx, candidate) in remaining.withIndex()) {
                // Hitung MMR Score
                val relevance = candidate.score
                
                // Hitung kemiripan maksimal dengan yang sudah terpilih
                var maxSim = 0.0
                for (sel in selected) {
                    val sim = cosineSimilarity(candidate.tokens, sel.tokens)
                    if (sim > maxSim) maxSim = sim
                }
                
                // Rumus MMR: Lambda * Relevansi - (1-Lambda) * Kemiripan
                val mmrScore = (lambda * relevance) - ((1 - lambda) * maxSim)
                
                if (mmrScore > bestScore) {
                    bestScore = mmrScore
                    bestIdx = idx
                }
            }
            
            if (bestIdx >= 0) {
                selected.add(remaining.removeAt(bestIdx))
            } else {
                break
            }
        }
        
        return selected
    }

    /**
     * Menghitung kesamaan kosinus antara dua set token (vektor biner).
     * Nilai 1.0 = identik, 0.0 = tidak ada irisan sama sekali.
     */
    private fun cosineSimilarity(setA: Set<String>, setB: Set<String>): Double {
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        
        val intersection = setA.intersect(setB).size.toDouble()
        val magnitudeA = sqrt(setA.size.toDouble())
        val magnitudeB = sqrt(setB.size.toDouble())
        
        return intersection / (magnitudeA * magnitudeB)
    }
}