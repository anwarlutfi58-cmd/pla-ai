package com.pla.agent

import java.io.File
import kotlin.random.Random

class MarkovComposer(private val folder: File) {
    private val transisi = HashMap<String, HashMap<String, Int>>()
    private val pembuka = ArrayList<String>()
    private var tandaAktif = ""
    private var siap = false

    fun isSiap(): Boolean = siap

    fun pastikanSiap(tanda: String, teksLatihan: List<String>): Boolean {
        if (siap && tanda == tandaAktif) return true
        if (muat(tanda)) return true
        return latih(teksLatihan, tanda)
    }

    private fun fileModel() = File(folder, "markov_model.txt")
    private fun filePembuka() = File(folder, "markov_pembuka.txt")
    private fun fileTanda() = File(folder, "markov_tanda.txt")

    private fun muat(tanda: String): Boolean {
        return try {
            val ft = fileTanda()
            if (!ft.exists() || ft.readText().trim() != tanda) return false
            transisi.clear()
            pembuka.clear()
            fileModel().forEachLine { baris ->
                val bagian = baris.split("\t")
                if (bagian.size == 3) {
                    transisi.getOrPut(bagian[0]) { HashMap() }[bagian[1]] = bagian[2].toIntOrNull() ?: 0
                }
            }
            filePembuka().forEachLine { if (it.isNotBlank()) pembuka.add(it) }
            siap = transisi.isNotEmpty() && pembuka.isNotEmpty()
            if (siap) tandaAktif = tanda
            siap
        } catch (e: Exception) {
            false
        }
    }

    private fun latih(teksList: List<String>, tanda: String): Boolean {
        return try {
            transisi.clear()
            pembuka.clear()
            for (teks in teksList) {
                val kalimatList = teks.lowercase().split(Regex("[.!?\\n]"))
                for (kalimat in kalimatList) {
                    val kata = kalimat.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }
                    if (kata.size < 3) continue
                    if (pembuka.size < 20000) pembuka.add(kata[0])
                    for (i in 0 until kata.size - 1) {
                        if (transisi.size > 40000) break
                        val tujuan = transisi.getOrPut(kata[i]) { HashMap() }
                        tujuan[kata[i + 1]] = (tujuan[kata[i + 1]] ?: 0) + 1
                    }
                }
            }
            if (transisi.isEmpty() || pembuka.isEmpty()) return false
            val sb = StringBuilder()
            for ((k1, peta) in transisi) {
                for ((k2, c) in peta) {
                    sb.append(k1).append("\t").append(k2).append("\t").append(c).append("\n")
                }
            }
            fileModel().writeText(sb.toString())
            filePembuka().writeText(pembuka.joinToString("\n"))
            fileTanda().writeText(tanda)
            siap = true
            tandaAktif = tanda
            true
        } catch (e: Exception) {
            false
        }
    }

    // Merangkai kalimat baru dari pola kata yang dipelajari dari dataset milik user
    fun rangkai(seed: List<String>, maxKata: Int = 26): String? {
        if (!siap) return null
        val seedSet = seed.toSet()
        var awal = pembuka.firstOrNull { it in seedSet }
        if (awal == null && pembuka.isNotEmpty()) awal = pembuka[Random.nextInt(pembuka.size)]
        if (awal == null) return null
        val hasil = StringBuilder(awal)
        var kini = awal
        var jumlah = 1
        while (jumlah < maxKata) {
            val pilihan = transisi[kini] ?: break
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
            if (berikutnya == null) break
            hasil.append(" ").append(berikutnya)
            kini = berikutnya
            jumlah++
        }
        if (jumlah < 4) return null
        return hasil.toString().replaceFirstChar { it.uppercase() } + "."
    }
}