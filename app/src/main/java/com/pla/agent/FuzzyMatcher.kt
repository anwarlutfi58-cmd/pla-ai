package com.pla.agent

object FuzzyMatcher {

    /**
     * Menghitung jarak edit (Levenshtein Distance) antara dua string.
     * Semakin kecil nilainya, semakin mirip kedua kata tersebut.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        // Optimasi cepat untuk kasus ekstrem
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        if (s1 == s2) return 0

        // Gunakan array 1D untuk hemat memori (penting untuk HP RAM 4GB)
        val prevRow = IntArray(len2 + 1) { it }
        val currRow = IntArray(len2 + 1)

        for (i in 1..len1) {
            currRow[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(
                    currRow[j - 1] + 1,      // Insert
                    prevRow[j] + 1,          // Delete
                    prevRow[j - 1] + cost    // Substitute
                )
            }
            System.arraycopy(currRow, 0, prevRow, 0, len2 + 1)
        }
        return prevRow[len2]
    }

    /**
     * Mencari kata dalam daftar yang paling mirip dengan input user.
     * @param threshold Jarak maksimal yang masih dianggap "mirip" (default: 2)
     * @return Kata terdekat jika ditemukan, null jika tidak ada yang cukup mirip
     */
    fun findClosestMatch(input: String, candidates: List<String>, threshold: Int = 2): String? {
        val lowerInput = input.lowercase()
        var bestMatch: String? = null
        var minDistance = Int.MAX_VALUE

        for (candidate in candidates) {
            val dist = levenshteinDistance(lowerInput, candidate.lowercase())
            if (dist < minDistance && dist <= threshold) {
                minDistance = dist
                bestMatch = candidate
            }
            // Early exit jika sudah exact match
            if (minDistance == 0) break
        }
        return bestMatch
    }

    /**
     * Mengembalikan skor kemiripan 0.0 - 1.0 berdasarkan jarak edit.
     */
    fun similarityScore(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen)
    }
}