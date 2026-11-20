package com.pla.agent

class LanguageAnalyzer {

    private val stopwords = setOf(
        "yang", "di", "ke", "dari", "untuk", "pada", "adalah", "itu", "ini",
        "saya", "kamu", "aku", "anda", "apa", "tolong", "bisa", "ada", "dan",
        "atau", "dengan", "dalam", "akan", "mau", "ingin", "kah", "lah", "pun",
        "nya", "sih", "dong", "ya", "the", "a", "an", "of", "to", "in", "is", "dan"
    )

    fun tokenize(text: String): List<String> {
        val cleanText = text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        return cleanText.split(Regex("\\s+")).filter { it.isNotEmpty() && !stopwords.contains(it) }
    }

    // Stemmer Bahasa Indonesia Sederhana (Menghapus imbuhan umum)
    fun stem(kata: String): String {
        var w = kata.lowercase()
        if (w.length <= 3) return w
        
        // Hapus sufiks
        for (s in listOf("kannya", "nya", "kan", "an", "i", "lah", "kah", "pun")) {
            if (w.endsWith(s) && w.length - s.length >= 3) {
                w = w.dropLast(s.length)
                break
            }
        }
        
        // Hapus prefiks (maksimal 3 kali iterasi untuk kata seperti "memperlihatkan")
        var ulang = 0
        while (ulang < 3) {
            val sebelum = w
            for (p in listOf(
                "memper", "mempen", "mempe", "mem", "meny", "men", "me",
                "peng", "peny", "pen", "pem", "pel", "per", "pe",
                "ber", "ter", "di", "ke", "se"
            )) {
                if (w.startsWith(p) && w.length - p.length >= 3) {
                    w = w.substring(p.length)
                    break
                }
            }
            if (w == sebelum) break
            ulang++
        }
        return w
    }

    fun tokenizeStemmed(text: String): List<String> = tokenize(text).map { stem(it) }.filter { it.length > 1 }

    // Algoritma Skor BM25-lite (Kata langka bernilai lebih tinggi, cocok di pertanyaan = bonus)
    fun hitungSkorPintar(
        inputTokens: List<String>,
        docTokens: Set<String>,
        questionTokens: Set<String>,
        idf: Map<String, Double>
    ): Double {
        var skor = 0.0
        for (t in inputTokens) {
            val bobot = idf[t] ?: 0.5
            if (docTokens.contains(t)) skor += bobot
            if (questionTokens.contains(t)) skor += bobot * 1.5 // Bonus besar jika cocok dengan kolom pertanyaan
        }
        return skor
    }
}