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
    private val fuzzyMatcher = FuzzyMatcher
    private val queryExpander = QueryExpander(context)
    private val knowledgeGraph = KnowledgeGraph(context)
    private val confidenceScorer = ConfidenceScorer
    private val reranker = ResponseReranker
    private val templateEngine = TemplateEngine
    private val contextMemory = ContextMemory(context)
    private val composer = NGramComposer(context.filesDir)
    private val preferences = AppPreferences(context)
    private val intentClassifier = IntentClassifier()

    private var cacheTanda = ""
    private var cacheMemori: List<MemoriTerurai> = emptyList()
    private var synonymBuilt = false
    private var graphBuilt = false

    private data class MemoriTerurai(
        val pertanyaan: String,
        val jawaban: String,
        val tokenGabungan: Set<String>,
        val tokenPertanyaan: Set<String>,
        val rawContent: String
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

        // 1. KLASIFIKASI NIAT & MUAT INGATAN PERCAKAPAN
        val intent = intentClassifier.classify(userInput)
        val lowerInput = userInput.lowercase().trim()
        val isContinuation = contextMemory.isLikelyContinuation(intent)

        // 2. SAPAAN & CUSTOM RULES (PONDASI LAMA - TETAP DIPERTAHANKAN)
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

        // 3. TOKENIZE + FUZZY CORRECTION + QUERY EXPANSION
        var inputTokens = analyzer.tokenizeStemmed(userInput)
        
        // Koreksi typo menggunakan FuzzyMatcher terhadap kosakata di dataset
        val allKnownWords = cacheMemori.flatMap { 
            it.tokenGabungan.toList() + it.tokenPertanyaan.toList() 
        }.distinct()
        
        inputTokens = inputTokens.map { token ->
            fuzzyMatcher.findClosestMatch(token, allKnownWords, threshold = 2) ?: token
        }

        // Perluas query dengan sinonim otomatis (hanya jika belum dibangun)
        if (!synonymBuilt) {
            onStatusUpdate("Membangun thesaurus dari datamu...")
            queryExpander.loadFromDisk()
            if (!queryExpander.isBuilt()) {
                queryExpander.buildFromMemories(cacheMemori.map { it.rawContent })
            }
            synonymBuilt = true
        }
        val expandedTokens = queryExpander.expandQuery(inputTokens).toList()

        // Gabungkan dengan ingatan percakapan jika lanjutan
        var finalTokens = expandedTokens.toMutableSet()
        if (isContinuation) {
            val contextTokens = contextMemory.getRelevantTokens()
            finalTokens.addAll(contextTokens)
            onStatusUpdate("Melanjutkan topik sebelumnya...")
        }

        if (finalTokens.isEmpty()) {
            kirimTeks("Silakan ketik pertanyaan yang lebih spesifik.", onToken)
            return@withContext
        }

        // 4. MUAT MEMORI DENGAN INCREMENTAL INDEX (FITUR #8)
        val memories = appDao.getMemoriesByVersion(activeVersion)
        val tandaCache = "$activeVersion|${memories.size}|${memories.firstOrNull()?.timestamp ?: 0}"
        
        if (tandaCache != cacheTanda) {
            cacheMemori = memories.mapNotNull { uraiMemori(it.content) }
            cacheTanda = tandaCache
            
            // Bangun Knowledge Graph secara inkremental hanya saat data berubah
            onStatusUpdate("Memperbarui graf pengetahuan...")
            knowledgeGraph.loadFromDisk()
            if (!knowledgeGraph.isBuilt()) {
                knowledgeGraph.buildFromMemories(cacheMemori.map { it.rawContent })
            }
            graphBuilt = true
            
            // Reset composer agar belajar ulang dari data baru
            composer.pastikanSiap(tandaCache, cacheMemori.map { it.jawaban })
        }

        if (cacheMemori.isEmpty()) {
            kirimTeks("Saya belum memiliki data di $activeVersion. Silakan tambahkan link data di Pengaturan.", onToken)
            return@withContext
        }

        // 5. HITUNG SKOR BM25 + CONFIDENCE SCORING
        val df = HashMap<String, Int>()
        for (m in cacheMemori) {
            for (t in m.tokenGabungan) df[t] = (df[t] ?: 0) + 1
        }
        val idf = HashMap<String, Double>()
        for ((kata, jumlah) in df) {
            idf[kata] = Math.log(1.0 + cacheMemori.size.toDouble() / jumlah.toDouble())
        }

        val candidates = mutableListOf<RankedMemory>()
        for (m in cacheMemori) {
            val bm25Score = analyzer.hitungSkorPintar(
                finalTokens.toList(), 
                m.tokenGabungan, 
                m.tokenPertanyaan, 
                idf
            )
            
            val hasQuestionMatch = m.tokenPertanyaan.any { it in finalTokens }
            val confidence = confidenceScorer.calculate(
                bm25Score = bm25Score,
                totalMemories = cacheMemori.size,
                matchedTokens = finalTokens.count { it in m.tokenGabungan },
                totalQueryTokens = finalTokens.size,
                hasQuestionMatch = hasQuestionMatch
            )
            
            // Hanya masukkan kandidat dengan keyakinan minimal MEDIUM
            if (confidence.level != ConfidenceLevel.NONE) {
                candidates.add(RankedMemory(m.jawaban, confidence.score, m.tokenGabungan))
            }
        }

        // 6. RERANKING UNTUK DIVERSITAS
        val topCandidates = reranker.rerank(candidates, lambda = 0.7, topK = 3)

        // 7. SUSUN JAWABAN FINAL
        val finalResponse: String
        
        if (topCandidates.isNotEmpty()) {
            val bestConfidence = confidenceScorer.calculate(
                bm25Score = topCandidates.first().score,
                totalMemories = cacheMemori.size,
                matchedTokens = finalTokens.count { it in cacheMemori.first().tokenGabungan },
                totalQueryTokens = finalTokens.size,
                hasQuestionMatch = cacheMemori.first().tokenPertanyaan.any { it in finalTokens }
            )
            
            val opening = templateEngine.getOpening(
                intent = intent,
                confidence = bestConfidence.level,
                sourceCount = topCandidates.size,
                isContinuation = isContinuation
            )
            
            val sb = StringBuilder(opening)
            sb.append(topCandidates.first().content.trim())
            
            // Tambahkan sumber kedua & ketiga jika beragam
            for (i in 1 until topCandidates.size) {
                sb.append(" Selain itu, ").append(topCandidates[i].content.trim())
            }
            
            // Saran pertanyaan terkait dari Knowledge Graph
            val suggestions = getSuggestions(finalTokens)
            if (suggestions.isNotEmpty()) {
                sb.append("\n\n💡 Kamu mungkin juga ingin tahu: \"").append(suggestions.random()).append("\"")
            }
            
            finalResponse = sb.toString()
            
            // Simpan ke ingatan percakapan
            contextMemory.add(finalTokens.toList(), intent)
            
        } else {
            // FALLBACK: N-GRAM COMPOSER
            if (!composer.isSiap()) onStatusUpdate("Mempelajari pola kalimat...")
            composer.pastikanSiap(tandaCache, cacheMemori.map { it.jawaban })
            
            val rangkaian = composer.rangkai(finalTokens.toList())
            finalResponse = if (rangkaian != null) {
                onStatusUpdate("Merangkai dari pola yang dipelajari...")
                "Saya belum punya data pasti tentang itu, tapi ini hasil rangkaian berdasarkan pola dari datamu:\n\n\"$rangkaian\"\n\n*(Catatan: Kalimat di atas adalah hasil rangkaian statistik, bukan fakta terverifikasi.)*"
            } else {
                "Maaf, saya belum mempelajari hal itu di $activeVersion. Silakan tambahkan link data di Pengaturan agar saya bisa belajar."
            }
            contextMemory.add(finalTokens.toList(), intent)
        }

        kirimTeks(finalResponse, onToken)
    }

    private fun getSuggestions(tokens: Set<String>): List<String> {
        val suggestions = mutableListOf<String>()
        for (token in tokens.take(3)) {
            val connected = knowledgeGraph.getConnectedNodes(token)
            for ((rel, nodes) in connected) {
                for (node in nodes.take(2)) {
                    suggestions.add("$token $rel $node")
                }
            }
        }
        return suggestions.distinct().take(3)
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
                tokenPertanyaan = analyzer.tokenizeStemmed(pertanyaan).toSet(),
                rawContent = content
            )
        } catch (e: Exception) {
            if (content.isBlank()) null
            else MemoriTerurai(
                pertanyaan = "",
                jawaban = content,
                tokenGabungan = analyzer.tokenizeStemmed(content).toSet(),
                tokenPertanyaan = emptySet(),
                rawContent = content
            )
        }
    }

    private suspend fun kirimTeks(teks: String, onToken: (String) -> Unit) {
        for (char in teks) {
            onToken(char.toString())
            Thread.sleep(15)
        }
    }
}