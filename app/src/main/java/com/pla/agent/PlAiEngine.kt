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
    private val intentClassifier = IntentClassifier()
    private val composer = NGramComposer(context.filesDir)
    private val preferences = AppPreferences(context)

    private var tokenPercakapanTerakhir: List<String> = emptyList()
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

        onStatusUpdate("Menganalisis niat...")
        val intent = intentClassifier.classify(userInput)
        val lowerInput = userInput.lowercase().trim()

        // 1. SAPAAN & CUSTOM RULES (PONDASI LAMA)
        val greetingResponse = when {
            lowerInput == "apa" || lowerInput == "apa?" -> "Assalamualaikum, ada yang bisa saya bantu?"
            lowerInput.contains("assalamualaikum") || lowerInput.contains("assalamu alaikum") -> "Waalaikumsalam warahmatullahi wabarakatuh. Ada yang bisa saya bantu?"
            lowerInput == "halo" || lowerInput == "hai" || lowerInput == "hello" -> "Halo! Saya $aiName. Ada yang bisa saya bantu hari ini?"
            lowerInput.contains("selamat pagi") -> "Selamat pagi! Semoga harimu menyenangkan."
            lowerInput.contains("terima kasih") || lowerInput.contains("makasih") -> "Sama-sama! Senang bisa membantu Anda."
            lowerInput.contains("siapa kamu") || lowerInput.contains("siapa nama kamu") -> "Saya $aiName, asisten AI mandiri yang siap membantu Anda belajar."
            else -> null
        }

        if (greetingResponse != null) {
            kirimTeks(greetingResponse, onToken)
            return@withContext
        }

        // 2. ANALISIS INPUT + KONTEKS PERCAKAPAN
        var inputTokens = analyzer.tokenizeStemmed(userInput)
        
        if (intent == UserIntent.CONTINUATION && tokenPercakapanTerakhir.isNotEmpty()) {
            inputTokens = (tokenPercakapanTerakhir + inputTokens).distinct()
            onStatusUpdate("Melanjutkan topik sebelumnya...")
        }

        if (inputTokens.isEmpty()) {
            kirimTeks("Silakan ketik pertanyaan yang lebih spesifik.", onToken)
            return@withContext
        }

        // 3. MUAT MEMORI (DENGAN CACHE)
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

        // 4. HITUNG SKOR BM25
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
        var s1 = 0.0; var s2 = 0.0; var s3 = 0.0

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

        // 5. SUSUN JAWABAN
        val finalResponse: String
        if (s1 > 0.5 && top1 != null) {
            val conf = minOf(98, 40 + (s1 * 10).toInt())
            onStatusUpdate("Data ditemukan (keyakinan $conf%)")

            val pembuka = when (intent) {
                UserIntent.EXPLANATION_REQUEST -> "Berikut penjelasannya berdasarkan catatan saya: "
                UserIntent.EXAMPLE_REQUEST -> "Berikut adalah contoh yang saya temukan: "
                else -> PEMBUKA_ACAK[(userInput.hashCode() and Int.MAX_VALUE) % PEMBUKA_ACAK.size]
            }

            val sb = StringBuilder(pembuka + top1.jawaban.trim())

            // Gabungkan dengan data terkait jika skornya mendekati
            if (top2 != null && s2 >= s1 * 0.65) sb.append(" Selain itu, ").append(top2.jawaban.trim())
            if (top3 != null && s3 >= s1 * 0.65) sb.append(" Terkait hal itu, ").append(top3.jawaban.trim())

            // Saran pertanyaan lanjutan
            val saran = when {
                top2 != null && top2.pertanyaan.isNotBlank() -> top2.pertanyaan
                top3 != null && top3.pertanyaan.isNotBlank() -> top3.pertanyaan
                else -> null
            }
            if (!saran.isNullOrBlank() && saran != top1.pertanyaan) {
                sb.append("\n\n💡 Kamu mungkin juga ingin tahu: \"").append(saran).append("\"")
            }

            finalResponse = sb.toString()
            tokenPercakapanTerakhir = inputTokens
        } else {
            // 6. FALLBACK: PERANGKAI KATA N-GRAM (Belajar dari datamu)
            if (!composer.isSiap()) onStatusUpdate("Mempelajari pola kalimat dari datamu...")
            composer.pastikanSiap(tandaCache, memori.map { it.jawaban })
            
            val rangkaian = composer.rangkai(inputTokens)
            finalResponse = if (rangkaian != null) {
                onStatusUpdate("Merangkai jawaban dari pola yang dipelajari...")
                "Saya belum punya data pasti tentang itu, tapi ini hasil rangkaian berdasarkan pola kalimat yang sudah saya pelajari dari datamu:\n\n\"$rangkaian\"\n\n*(Catatan: Kalimat di atas adalah hasil rangkaian statistik, bukan fakta terverifikasi.)*"
            } else {
                "Maaf, saya belum mempelajari hal itu di $activeVersion. Silakan tambahkan link data di Pengaturan agar saya bisa belajar."
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
            Thread.sleep(15) // Kecepatan mengetik yang nyaman
        }
    }

    companion object {
        private val PEMBUKA_ACAK = listOf(
            "Berdasarkan catatan saya, ",
            "Menurut data yang sudah saya pelajari, ",
            "Dari hasil belajar saya, ",
            "Saya menemukan informasi ini: ",
            "Berikut yang saya tahu tentang hal tersebut: "
        )
    }
}