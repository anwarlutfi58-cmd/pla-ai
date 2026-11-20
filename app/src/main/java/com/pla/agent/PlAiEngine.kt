package com.pla.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PlAiEngine(
    private val context: Context,
    private val appDao: AppDao
) {
    private val analyzer = LanguageAnalyzer()
    private val preferences = AppPreferences(context)
    private val composer = MarkovComposer(context.filesDir)

    // Ingatan percakapan jangka pendek (fitur baru)
    private var tokenPercakapanTerakhir: List<String> = emptyList()

    // Cache memori agar JSON tidak diurai ulang tiap pertanyaan (fitur baru)
    private var cacheTanda = ""
    private var cacheMemori: List<MemoriTerurai> = emptyList()

    private data class MemoriTerurai(
        val pertanyaan: String,
        val jawaban: String,
        val tokenGabungan: Set<String>,
        val tokenPertanyaan: Set<String>
    )

    fun isLoaded(): Boolean = true
    fun unloadModel() {}

    suspend fun generateResponse(
        userInput: String,
        onStatusUpdate: (String) -> Unit,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {

        val activeVersion = appDao.getActiveVersion()?.versionName ?: "V1. pla"
        val aiName = preferences.getAiName()

        onStatusUpdate("Menganalisis dengan $activeVersion...")

        val lowerInput = userInput.lowercase().trim()

        // ==========================================
        // 1. SAPAAN & CUSTOM RULES (PONDASI LAMA - TIDAK DIUBAH)
        // ==========================================
        val greetingResponse = when {
            lowerInput == "apa" || lowerInput == "apa?" ->
                "Assalamualaikum, ada yang bisa saya bantu?"
            lowerInput.contains("assalamualaikum") || lowerInput.contains("assalamu alaikum") ->
                "Waalaikumsalam warahmatullahi wabarakatuh. Ada yang bisa saya bantu?"
            lowerInput == "halo" || lowerInput == "hai" || lowerInput == "hello" ->
                "Halo! Saya $aiName. Ada yang bisa saya bantu hari ini?"
            lowerInput.contains("selamat pagi") ->
                "Selamat pagi! Semoga harimu menyenangkan. Ada yang bisa saya bantu?"
            lowerInput.contains("selamat siang") ->
                "Selamat siang! Ada yang bisa saya bantu?"
            lowerInput.contains("selamat malam") ->
                "Selamat malam! Ada yang bisa saya bantu?"
            lowerInput.contains("terima kasih") ->
                "Sama-sama! Senang bisa membantu Anda."
            lowerInput.contains("siapa kamu") || lowerInput.contains("siapa nama kamu") ->
                "Saya $aiName, asisten AI mandiri yang siap membantu Anda belajar."
            else -> null
        }

        if (greetingResponse != null) {
            kirimTeks(greetingResponse, onToken)
            return@withContext
        }

        // ==========================================
        // 2. ANALISIS INPUT + KONTEKS PERCAKAPAN (FITUR BARU)
        // ==========================================
        var inputTokens = analyzer.tokenizeStemmed(userInput)

        val kataLanjutan = listOf("lanjutkan", "jelaskan", "detail", "terus", "kenapa", "mengapa", "contoh", "misal", "lagi", "itu", "tadi", "maksud")
        val adalahLanjutan = inputTokens.size <= 3 && kataLanjutan.any { lowerInput.contains(it) }
        if (adalahLanjutan && tokenPercakapanTerakhir.isNotEmpty()) {
            inputTokens = (tokenPercakapanTerakhir + inputTokens).distinct()
            onStatusUpdate("Melanjutkan topik sebelumnya...")
        }

        if (inputTokens.isEmpty()) {
            kirimTeks("Silakan ketik pertanyaan Anda.", onToken)
            return@withContext
        }

        // ==========================================
        // 3. MUAT & URAI MEMORI (dengan cache)
        // ==========================================
        val memories = appDao.getMemoriesByVersion(activeVersion)
        val tandaCache = "$activeVersion|${memories.size}|${memories.firstOrNull()?.timestamp ?: 0}"
        if (tandaCache != cacheTanda) {
            cacheMemori = memories.mapNotNull { uraiMemori(it.content) }
            cacheTanda = tandaCache
        }
        val memori = cacheMemori

        if (memori.isEmpty()) {
            kirimTeks("Saya belum memiliki data di $activeVersion. Silakan tambahkan link data di Pengaturan.", onToken)
            return@withContext
        }

        // ==========================================
        // 4. SKOR BERBOBOT (FITUR BARU)
        // ==========================================
        val df = HashMap<String, Int>()
        for (m in memori) {
            for (t in m.tokenGabungan) df[t] = (df[t] ?: 0) + 1
        }
        val idf = HashMap<String, Double>()
        for ((kata, jumlah) in df) {
            idf[kata] = Math.log(1.0 + memori.size.toDouble() / jumlah.toDouble())
        }

        var top1: MemoriTerurai? = null
        var top2: MemoriTerurai? = null
        var top3: MemoriTerurai? = null
        var s1 = 0.0
        var s2 = 0.0
        var s3 = 0.0

        for (m in memori) {
            val skor = analyzer.hitungSkorPintar(inputTokens, m.tokenGabungan, m.tokenPertanyaan, idf)
            if (skor > s1) {
                s3 = s2; top3 = top2
                s2 = s1; top2 = top1
                s1 = skor; top1 = m
            } else if (skor > s2) {
                s3 = s2; top3 = top2
                s2 = skor; top2 = m
            } else if (skor > s3) {
                s3 = skor; top3 = m
            }
        }

        val finalResponse: String
        if (s1 > 0 && top1 != null) {
            val conf = minOf(97, 30 + (s1 * 8).toInt())
            onStatusUpdate("Ingatan ditemukan di $activeVersion (keyakinan $conf%)")

            val pembuka = PEMBUKA[(userInput.hashCode() and Int.MAX_VALUE) % PEMBUKA.size]
            val sb = StringBuilder(pembuka + top1.jawaban.trim())

            val pakai2 = top2 != null && s2 >= s1 * 0.6
            val pakai3 = top3 != null && s3 >= s1 * 0.6
            if (pakai2 && top2 != null) sb.append(" Selain itu, ").append(top2.jawaban.trim())
            if (pakai3 && top3 != null) sb.append(" Terkait hal itu, ").append(top3.jawaban.trim())

            val saran = when {
                !pakai2 && top2 != null && top2.pertanyaan.isNotBlank() -> top2.pertanyaan
                !pakai3 && top3 != null && top3.pertanyaan.isNotBlank() -> top3.pertanyaan
                else -> null
            }
            if (!saran.isNullOrBlank()) {
                sb.append("\n\nMungkin kamu juga ingin tahu: \"").append(saran).append("\"")
            }

            finalResponse = sb.toString()
            tokenPercakapanTerakhir = inputTokens
        } else {
            // ==========================================
            // 5. FALLBACK: PERANGKAI KATA MARKOV (FITUR BARU)
            // ==========================================
            if (!composer.isSiap()) onStatusUpdate("Belajar pola kata dari datamu...")
            composer.pastikanSiap(tandaCache, memori.map { it.jawaban })
            val rangkaian = composer.rangkai(inputTokens)
            finalResponse = if (rangkaian != null) {
                onStatusUpdate("Merangkai kata dari pola yang dipelajari...")
                "Saya belum punya data pasti tentang itu, tapi ini hasil rangkaian saya dari pola kata yang saya pelajari:\n\n$rangkaian\n\n(Kalimat di atas adalah rangkaian pola dari datamu, bukan fakta terverifikasi.)"
            } else {
                "Maaf, saya belum mempelajari hal itu di $activeVersion. Silakan tambahkan link data di Pengaturan."
            }
            tokenPercakapanTerakhir = inputTokens
        }

        kirimTeks(finalResponse, onToken)
    }

    private fun uraiMemori(content: String): MemoriTerurai? {
        return try {
            val obj = JSONObject(content)
            val pertanyaan = obj.optString("pertanyaan", "")
            val jawaban = obj.optString("jawaban", obj.optString("isi", ""))
            val topik = obj.optString("topik", "")
            val kategori = obj.optString("kategori", "")
            val gabungan = "$pertanyaan $jawaban $topik $kategori"
            if (jawaban.isBlank() && pertanyaan.isBlank()) null
            else MemoriTerurai(
                pertanyaan = pertanyaan,
                jawaban = if (jawaban.isNotBlank()) jawaban else gabungan,
                tokenGabungan = analyzer.tokenizeStemmed(gabungan).toSet(),
                tokenPertanyaan = analyzer.tokenizeStemmed(pertanyaan).toSet()
            )
        } catch (e: Exception) {
            if (content.isBlank()) null
            else MemoriTerurai(
                pertanyaan = "",
                jawaban = content,
                tokenGabungan = analyzer.tokenizeStemmed(content).toSet(),
                tokenPertanyaan = emptySet()
            )
        }
    }

    private suspend fun kirimTeks(teks: String, onToken: (String) -> Unit) {
        for (char in teks) {
            onToken(char.toString())
            Thread.sleep(20)
        }
    }

    companion object {
        private val PEMBUKA = listOf(
            "Berdasarkan catatan saya, ",
            "Menurut data yang sudah saya pelajari, ",
            "Dari hasil belajar saya, ",
            "Saya menemukan ini di catatan saya: "
        )
    }
}