package com.pla.agent

data class ConfidenceResult(
    val score: Double,        // 0.0 - 1.0
    val level: ConfidenceLevel,
    val explanation: String   // Alasan internal (untuk debug/logging)
)

enum class ConfidenceLevel {
    HIGH,      // > 0.7 : Jawaban sangat relevan, AI yakin
    MEDIUM,    // 0.4-0.7 : Relevan tapi mungkin perlu verifikasi
    LOW,       // 0.2-0.4 : Kemiripan lemah, gunakan dengan hati-hati
    NONE       // < 0.2 : Tidak relevan, jangan jawab
}

object ConfidenceScorer {
    
    /**
     * Menghitung skor keyakinan berdasarkan:
     * 1. Skor BM25 mentah
     * 2. Ukuran dataset (normalisasi)
     * 3. Jumlah token yang cocok vs total token query
     * 4. Kehadiran kata kunci pertanyaan di jawaban
     */
    fun calculate(
        bm25Score: Double,
        totalMemories: Int,
        matchedTokens: Int,
        totalQueryTokens: Int,
        hasQuestionMatch: Boolean
    ): ConfidenceResult {
        
        // Normalisasi skor BM25 berdasarkan ukuran dataset
        // Dataset besar = skor tinggi lebih sulit dicapai
        val normalizedBm25 = when {
            totalMemories < 100 -> bm25Score / 5.0
            totalMemories < 1000 -> bm25Score / 10.0
            totalMemories < 10000 -> bm25Score / 20.0
            else -> bm25Score / 30.0
        }
        
        // Rasio token yang cocok
        val tokenRatio = if (totalQueryTokens > 0) {
            matchedTokens.toDouble() / totalQueryTokens
        } else 0.0
        
        // Bonus jika kolom pertanyaan juga cocok (sinyal kuat)
        val questionBonus = if (hasQuestionMatch) 0.15 else 0.0
        
        // Gabungkan dengan bobot
        var rawScore = (normalizedBm25 * 0.5) + (tokenRatio * 0.35) + questionBonus
        
        // Clamp ke 0-1
        rawScore = rawScore.coerceIn(0.0, 1.0)
        
        // Tentukan level
        val level = when {
            rawScore >= 0.7 -> ConfidenceLevel.HIGH
            rawScore >= 0.4 -> ConfidenceLevel.MEDIUM
            rawScore >= 0.2 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.NONE
        }
        
        val explanation = buildString {
            append("BM25_norm=${String.format("%.2f", normalizedBm25)}, ")
            append("TokenRatio=${String.format("%.2f", tokenRatio)}, ")
            append("QMatch=$hasQuestionMatch, ")
            append("DatasetSize=$totalMemories")
        }
        
        return ConfidenceResult(rawScore, level, explanation)
    }

    /**
     * Threshold adaptif: jika semua kandidat skornya rendah,
     * naikkan standar agar AI tidak memaksakan jawaban buruk.
     */
    fun getAdaptiveThreshold(topScores: List<Double>): Double {
        if (topScores.isEmpty()) return 0.4
        
        val avg = topScores.average()
        val max = topScores.max()
        
        // Jika rata-rata sangat rendah, berarti dataset belum cukup
        // Naikkan threshold agar AI lebih konservatif
        return when {
            avg < 0.1 -> 0.5  // Dataset masih kecil/kosong
            max < 0.3 -> 0.4  // Tidak ada yang benar-benar cocok
            else -> 0.2       // Normal
        }
    }
}