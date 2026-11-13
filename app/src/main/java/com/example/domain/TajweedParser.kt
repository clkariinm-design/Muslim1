package com.nuur.muslim.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.nuur.muslim.ui.theme.TajweedPalette

data class TajweedRuleSpec(
    val code: String,
    val nameSo: String,
    val nameAr: String,
    val nameEn: String,
    val color: Color
)

object TajweedParser {

    // Regex matching AlQuran Cloud bracket syntax: [tag[:id][content]]
    private val TAG_REGEX = Regex("""\[([a-zA-Z]+)(?::\d+)?\[([^\]]*)\]""")

    val rules: List<TajweedRuleSpec> = listOf(
        TajweedRuleSpec("h", "Hamzat ul-Wasl (Aamusan)", "همزة الوصل", "Hamzat ul-Wasl (Silent)", TajweedPalette.SilentOrWasl),
        TajweedRuleSpec("l", "Laam Shamsiyyah", "لام شمسية", "Laam Shamsiyyah", TajweedPalette.SilentOrWasl),
        TajweedRuleSpec("s", "Xaraf Aamusan", "حرف ساكن / صامت", "Silent Letter", TajweedPalette.SilentOrWasl),
        TajweedRuleSpec("n", "Madda Caadi ah (2)", "مد طبيعي", "Madda Normal (2)", TajweedPalette.MaddaNormal),
        TajweedRuleSpec("p", "Madda Banaan (2/4/6)", "مد جائز / عارض", "Madda Permissible (2/4/6)", TajweedPalette.MaddaPermissible),
        TajweedRuleSpec("o", "Madda Waajib ah (4/5)", "مد متصل / منفصل", "Madda Obligatory (4/5)", TajweedPalette.MaddaObligatory),
        TajweedRuleSpec("m", "Madda Laazim ah (6)", "مد لازم", "Madda Necessary (6)", TajweedPalette.MaddaNecessary),
        TajweedRuleSpec("q", "Qalqalah (Dhawaq)", "قلقلة", "Qalqalah", TajweedPalette.Qalqalah),
        TajweedRuleSpec("g", "Ghunnah (San-ku-nuqul)", "غنة", "Ghunnah", TajweedPalette.Ghunnah),
        TajweedRuleSpec("f", "Ikhfaa", "إخفاء", "Ikhafa", TajweedPalette.Ikhafa),
        TajweedRuleSpec("c", "Ikhfaa Shafawi", "إخفاء شفوي", "Ikhafa Shafawi", TajweedPalette.IkhafaShafawi),
        TajweedRuleSpec("i", "Iqlaab", "إقلاب", "Iqlab", TajweedPalette.Iqlab),
        TajweedRuleSpec("a", "Idghaam leh Ghunnah", "إدغام بغنة", "Idgham With Ghunnah", TajweedPalette.IdghamGhunnah),
        TajweedRuleSpec("u", "Idghaam bilaa Ghunnah", "إدغام بلا غنة", "Idgham Without Ghunnah", TajweedPalette.IdghamNoGhunnah),
        TajweedRuleSpec("w", "Idghaam Shafawi", "إدغام شفوي", "Idgham Shafawi", TajweedPalette.IdghamShafawi),
        TajweedRuleSpec("b", "Idghaam Mutaqaaribayn", "إدغام متقاربين", "Idgham Mutaqaribayn", TajweedPalette.IdghamMute),
        TajweedRuleSpec("d", "Idghaam Mutajaanisayn", "إدغام متجانسين", "Idgham Mutajanisayn", TajweedPalette.IdghamMute)
    )

    private val ruleMap: Map<String, TajweedRuleSpec> = rules.associateBy { it.code }

    fun colorForCode(code: String): Color = ruleMap[code]?.color ?: Color.Unspecified

    fun parseToAnnotatedString(tajweedRaw: String): AnnotatedString {
        return buildAnnotatedString {
            var cursor = 0
            for (match in TAG_REGEX.findAll(tajweedRaw)) {
                val start = match.range.first
                if (start > cursor) {
                    append(tajweedRaw.substring(cursor, start))
                }
                val code = match.groupValues[1]
                val inner = match.groupValues[2]
                val color = colorForCode(code)
                if (color != Color.Unspecified) {
                    pushStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium))
                    append(inner)
                    pop()
                } else {
                    append(inner)
                }
                cursor = match.range.last + 1
            }
            if (cursor < tajweedRaw.length) {
                append(tajweedRaw.substring(cursor))
            }
        }
    }

    fun stripTags(tajweedRaw: String): String {
        return TAG_REGEX.replace(tajweedRaw) { it.groupValues[2] }
    }
}
