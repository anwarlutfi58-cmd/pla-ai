package com.pla.agent

object TemplateEngine {

    enum class Tone {
        FORMAL,       // Untuk jawaban fakta dengan keyakinan tinggi
        EXPLANATORY,  // Untuk permintaan penjelasan
        CASUAL,       // Untuk obrolan atau jawaban keyakinan sedang
        CAUTIOUS,     // Untuk jawaban keyakinan rendah
        CONTINUATION  // Untuk lanjutan percakapan
    }

    /**
     * Memilih template pembuka yang paling sesuai dengan konteks saat ini.
     */
    fun getOpening(intent: UserIntent, confidence: ConfidenceLevel, sourceCount: Int, isContinuation: Boolean): String {
        // Jika lanjutan percakapan, gunakan transisi natural
        if (isContinuation) {
            return CONTINUATION_TEMPLATES.random()
        }

        // Tentukan nada bicara berdasarkan kombinasi intent & confidence
        val tone = when {
            intent == UserIntent.EXPLANATION_REQUEST -> Tone.EXPLANATORY
            intent == UserIntent.CHITCHAT -> Tone.CASUAL
            confidence == ConfidenceLevel.HIGH && intent == UserIntent.FACT_QUESTION -> Tone.FORMAL
            confidence == ConfidenceLevel.LOW || confidence == ConfidenceLevel.NONE -> Tone.CAUTIOUS
            else -> Tone.CASUAL
        }

        return when (tone) {
            Tone.FORMAL -> {
                if (sourceCount > 1) FORMAL_MULTI_SOURCE.random()
                else FORMAL_SINGLE_SOURCE.random()
            }
            Tone.EXPLANATORY -> EXPLANATORY_TEMPLATES.random()
            Tone.CASUAL -> CASUAL_TEMPLATES.random()
            Tone.CAUTIOUS -> CAUTIOUS_TEMPLATES.random()
            Tone.CONTINUATION -> CONTINUATION_TEMPLATES.random()
        }
    }

    private val FORMAL_SINGLE_SOURCE = listOf(
        "Berdasarkan catatan saya, ",
        "Menurut data yang tersimpan, ",
        "Informasi yang saya miliki menunjukkan bahwa "
    )

    private val FORMAL_MULTI_SOURCE = listOf(
        "Dari beberapa sumber yang saya pelajari, ",
        "Berdasarkan sintesis data dari berbagai catatan, ",
        "Saya menemukan informasi konsisten dari beberapa sumber: "
    )

    private val EXPLANATORY_TEMPLATES = listOf(
        "Mari saya jelaskan berdasarkan catatan saya: ",
        "Berikut penjelasan lengkap dari data yang ada: ",
        "Untuk memahami hal ini, catatan saya menyebutkan: "
    )

    private val CASUAL_TEMPLATES = listOf(
        "Oke, ini yang saya tahu: ",
        "Nah, dari belajar saya kemarin, ",
        "Hmm, kalau lihat datanya sih begini: "
    )

    private val CAUTIOUS_TEMPLATES = listOf(
        "Saya kurang yakin, tapi sepertinya: ",
        "Data saya terbatas, namun ada kemungkinan: ",
        "Ini belum sepenuhnya terverifikasi, tapi: "
    )

    private val CONTINUATION_TEMPLATES = listOf(
        "Melanjutkan pembahasan tadi, ",
        "Terkait hal itu, saya juga menemukan: ",
        "Oh ya, tambahan dari catatan sebelumnya: ",
        "Sambungan dari yang tadi, "
    )
}