package com.pla.agent

import java.io.File
import kotlin.random.Random

class NGramComposer(private val folder: File) {
    private val trigramModel = HashMap<String, HashMap<String, Int>>() // "kata1 kata2" -> {"kata3": jumlah}
    private val pembuka = ArrayList<String>()
    private var tandaAktif = ""
    private var siap = false

    fun isSiap(): Boolean = siap

    fun pastikanSiap(tanda: String, teksLatihan: List<String>): Boolean {
        if (siap && tanda == tandaAktif) return true
        if (muat(tanda)) return true
        return latih(teksLatihan, tanda)
    }

    private fun fileModel() = File(folder, "ngram_model.txt")
    private fun filePembuka() = File(folder, "ngram_pembuka.txt")
    private fun fileTanda() = File(folder, "ngram_tanda.txt")

    private fun muat(tanda: String): Boolean {
        return try {
            val ft = fileTanda()
            if (!ft.exists() || ft.readText().trim() != tanda) return false
            
            trigramModel.clear()
            pembuka.clear()
            
            fileModel().forEachLine { baris ->
                val bagian = baris.split("\t")
                if (bagian.size == 3) {
                    trigramModel.getOrPut(bagian[0]) { HashMap() }[bagian[1]] = bagian[2].toIntOrNull() ?: 0
                }
            }
            filePembuka().forEachLine { if (it.isNotBlank()) pembuka.add(it) }
            
            siap = trigramModel.isNotEmpty() && pembuka.isNotEmpty()
            if (siap) tandaAktif = tanda
            siap
        } catch (e: Exception) { false }
    }

    private fun latih(teksList: List<String>, tanda: String): Boolean {
        return try {
            trigramModel.clear()
            pembuka.clear()
            
            for (teks in teksList) {
                val kalimatList = teks.lowercase().split(Regex("[.!?\\n]+"))
                for (kalimat in kalimatList) {
                    val kata = kalimat.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }
                    if (kata.size < 3) continue
                    
                    if (pembuka.size < 15000) pembuka.add("${kata[0]} ${kata[1]}")
                    
                    for (i in 0 until kata.size - 2) {
                        if (trigramModel.size > 30000) break // Batas memori aman untuk RAM 4GB
                        val kunci = "${kata[i]} ${kata[i+1]}"
                        val berikutnya = kata[i+2]
                        val tujuan = trigramModel.getOrPut(kunci) { HashMap() }
                        tujuan[berikutnya] = (tujuan[berikutnya] ?: 0) + 1
                    }
                }
            }
            
            if (trigramModel.isEmpty() || pembuka.isEmpty()) return false
            
            val sb = StringBuilder()
            for ((kunci, peta) in trigramModel) {
                for ((kataLanjut, c) in peta) {
                    sb.append(kunci).append("\t").append(kataLanjut).append("\t").append(c).append("\n")
                }
            }
            fileModel().writeText(sb.toString())
            filePembuka().writeText(pembuka.joinToString("\n"))
            fileTanda().writeText(tanda)
            
            siap = true
            tandaAktif = tanda
            true
        } catch (e: Exception) { false }
    }

    fun rangkai(seedTokens: List<String>, maxKata: Int = 30): String? {
        if (!siap) return null
        
        val seedSet = seedTokens.toSet()
        var awal = pembuka.firstOrNull { it.split(" ").any { k -> k in seedSet } }
        
        if (awal == null && pembuka.isNotEmpty()) {
            awal = pembuka[Random.nextInt(pembuka.size)]
        }
        if (awal == null) return null
        
        val hasil = StringBuilder(awal.replace(" ", " "))
        var bagian = awal.split(" ")
        var jumlah = 2
        
        while (jumlah < maxKata) {
            val kunci = "${bagian[0]} ${bagian[1]}"
            val pilihan = trigramModel[kunci] ?: break
            
            var total = 0
            for (c in pilihan.values) total += c
            if (total <= 0) break
            
            var acak = Random.nextInt(total)
            var berikutnya: String? = null
            
            for ((k, c) in pilihan) {
                acak -= c
                if (acak < 0) {
                    berikutnya = k
                    break
                }
            }
            
            if (berikutnya == null || berikutnya == "." || berikutnya == "," || berikutnya == "!") break
            
            hasil.append(" ").append(berikutnya)
            bagian = listOf(bagian[1], berikutnya)
            jumlah++
        }
        
        if (jumlah < 5) return null
        return hasil.toString().replaceFirstChar { it.uppercase() } + "."
    }
}