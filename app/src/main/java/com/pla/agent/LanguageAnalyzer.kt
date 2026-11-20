package com.pla.agent

class LanguageAnalyzer {

    // Kata yang diabaikan agar AI fokus pada kata penting
    private val stopwords = setOf(
        "yang", "di", "ke", "dari", "untuk", "pada", "adalah", "itu", "ini",
        "saya", "kamu", "aku", "anda", "apa", "tolong", "bisa", "ada", "dan",
        "atau", "dengan", "dalam", "akan", "mau", "ingin", "kah", "lah", "pun",
        "nya", "sih", "dong", "ya", "the", "a", "an", "of", "to", "in", "is"
    )

    // FUNGSI LAMA (pondasi) - diperbaiki pemisah katanya
    fun tokenize(text: String): List<String> {
        val cleanText = text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        return cleanText.split(Regex("\\s+"))
            .filter { it.isNotEmpty() && !stopwords.contains(it) }
    }

    // FUNGSI LAMA (pondasi) - tetap ada agar file lain tidak error
    fun hitungSkorKecocokan(inputTokens: List<String>, memoryText: String): Int {
        val memoryTokens = tokenize(memoryText)
        var skor = 0
        for (token in inputTokens) {
            if (memoryTokens.contains(token)) skor++
        }
        return skor
    }

    // ===== FITUR BARU: Stemmer Bahasa Indonesia sederhana =====
    fun stem(kata: String): String {
        var w = kata.lowercase()
        if (w.length <= 3) return w
        for (s in listOf("kannya", "nya", "kan", "an", "i")) {
            if (w.endsWith(s) && w.length - s.length >= 3) {
                w = w.dropLast(s.length)
                break
            }
        }
        var ulang = 0
        while (ulang < 3) {
            val sebelum = w
            for (p in listOf(
                "meng", "meny", "men", "mem", "me",
                "peng", "peny", "pen", "pem", "pel", "per", "pe",
                "ter", "ber", "bel", "be", "di", "ke", "se"
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

    fun tokenizeStemmed(text: String): List<String> = tokenize(text).map { stem(it) }

    // ===== FITUR BARU: Skor berbobot (kata langka = bobot besar, cocok pertanyaan = bonus) =====
    fun hitungSkorPintar(
        inputTokens: List<String>,
        memoryTokens: Set<String>,
        questionTokens: Set<String>,
        idf: Map<String, Double>
    ): Double {
        var skor = 0.0
        for (t in inputTokens) {
            if (memoryTokens.contains(t)) skor += (idf[t] ?: 1.0)
            if (questionTokens.contains(t)) skor += (idf[t] ?: 1.0) * 1.5
        }
        return skor
    }
}