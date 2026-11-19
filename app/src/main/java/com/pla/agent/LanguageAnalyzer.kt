package com.pla.agent

class LanguageAnalyzer {

    // Daftar kata yang diabaikan (Stopwords) agar AI fokus pada kata penting
    private val stopwords = setOf(
        "yang", "di", "ke", "dari", "untuk", "pada", "adalah", "itu", "ini", 
        "saya", "kamu", "aku", "anda", "apa", "tolong", "bisa", "ada", "dan", 
        "atau", "dengan", "dalam", "akan", "mau", "ingin"
    )

    // Memecah kalimat menjadi kata-kata penting
    fun tokenize(text: String): List<String> {
        // 1. Ubah ke huruf kecil dan hapus tanda baca
        val cleanText = text.lowercase().replace(Regex("[^a-z0-9\\s]"), "")
        
        // 2. Pecah berdasarkan spasi
        val words = cleanText.split(Regex("\\s+"))
        
        // 3. Buang stopwords dan kata yang kosong
        return words.filter { it.isNotEmpty() && !stopwords.contains(it) }
    }

    // Menghitung skor kecocokan antara pertanyaan user dan ingatan AI
    fun hitungSkorKecocokan(inputTokens: List<String>, memoryText: String): Int {
        val memoryTokens = tokenize(memoryText)
        var skor = 0
        
        // Semakin banyak kata yang sama, semakin tinggi skornya
        for (token in inputTokens) {
            if (memoryTokens.contains(token)) {
                skor++
            }
        }
        return skor
    }
}