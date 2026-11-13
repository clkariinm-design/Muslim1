package com.nuur.muslim.domain

import java.io.File

data class ReciterInfo(
    val id: String,
    val englishName: String,
    val arabicName: String,
    val bitrate: Int
)

object RecitersCatalog {
    val reciters: List<ReciterInfo> = listOf(
        ReciterInfo("ar.alafasy", "Mishary Rashid Alafasy", "مشاري راشد العفاسي", 128),
        ReciterInfo("ar.abdulbasitmurattal", "Abdul Basit (Murattal)", "عبد الباسط عبد الصمد (مرتل)", 192),
        ReciterInfo("ar.abdurrahmaansudais", "Abdurrahmaan As-Sudais", "عبد الرحمن السديس", 192),
        ReciterInfo("ar.husary", "Mahmoud Khalil Al-Husary", "محمود خليل الحصري", 128),
        ReciterInfo("ar.mahermuaiqly", "Maher Al Muaiqly", "ماهر المعيقلي", 128),
        ReciterInfo("ar.shaatree", "Abu Bakr Ash-Shatri", "أبو بكر الشاطري", 128),
        ReciterInfo("ar.saoodshuraym", "Saood Ash-Shuraim", "سعود الشريم", 64)
    )

    fun findById(id: String): ReciterInfo = reciters.firstOrNull { it.id == id } ?: reciters.first()

    fun audioUrlForAyah(reciterId: String, globalAyahNumber: Int): String {
        val reciter = findById(reciterId)
        return "https://cdn.islamic.network/quran/audio/${reciter.bitrate}/${reciter.id}/$globalAyahNumber.mp3"
    }

    fun localAudioFile(filesDir: File, reciterId: String, globalAyahNumber: Int): File {
        val dir = File(filesDir, "audio/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$globalAyahNumber.mp3")
    }
}
