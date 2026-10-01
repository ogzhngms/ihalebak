package com.ogzhngms.ihalebak.ui

import com.ogzhngms.ihalebak.Tender
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")
private val LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TURKISH)
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", TURKISH)
private val UPDATED = DateTimeFormatter.ofPattern("d MMMM HH:mm", TURKISH)

fun Tender.whenText(long: Boolean = false): String? {
    val day = date ?: return null
    val text = day.format(if (long) LONG_DATE else SHORT_DATE)
    return if (time != null) "$text · $time" else text
}

fun daysLeftText(days: Long?): String? = when {
    days == null -> null
    days < 0 -> "Geçti"
    days == 0L -> "Bugün"
    days == 1L -> "Yarın"
    else -> "$days gün"
}

fun LocalDate.shortText(): String = format(SHORT_DATE)

// "2026-10-01T13:30+03:00" from index.json, shown as "1 Ekim 13:30".
fun updatedText(iso: String): String = runCatching { OffsetDateTime.parse(iso).format(UPDATED) }.getOrDefault(iso)
