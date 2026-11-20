package com.pla.agent

enum class UserIntent {
    FACT_QUESTION,      // Bertanya fakta ("Apa itu...", "Siapa...")
    EXPLANATION_REQUEST,// Minta penjelasan ("Jelaskan...", "Kenapa...", "Mengapa...")
    EXAMPLE_REQUEST,    // Minta contoh ("Contoh...", "Misalnya...")
    CHITCHAT,           // Obrolan ringan ("Halo", "Terima kasih")
    CONTINUATION,       // Lanjutan ("Lanjutkan", "Terus", "Lagi")
    UNKNOWN
}

class IntentClassifier {
    fun classify(text: String): UserIntent {
        val lower = text.lowercase().trim()
        return when {
            lower.matches(Regex(".*(halo|hai|hello|pagi|siang|malam|terima kasih|makasih).*")) -> UserIntent.CHITCHAT
            lower.matches(Regex("^(apa|siapa|dimana|kapan|berapa|bagaimana).*")) -> UserIntent.FACT_QUESTION
            lower.matches(Regex(".*(jelaskan|kenapa|mengapa|ceritakan|deskripsikan).*")) -> UserIntent.EXPLANATION_REQUEST
            lower.matches(Regex(".*(contoh|misalnya|misal|seperti apa).*")) -> UserIntent.EXAMPLE_REQUEST
            lower.matches(Regex("^(lanjut|terus|lagi|itu|tadi|maksud).*")) -> UserIntent.CONTINUATION
            else -> UserIntent.UNKNOWN
        }
    }
}