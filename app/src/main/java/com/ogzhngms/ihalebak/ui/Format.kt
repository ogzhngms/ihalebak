package com.ogzhngms.ihalebak.ui

import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.isOver
import java.text.Collator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")

// Alphabetical the Turkish way: Ç after C, İ after I, Ş after S.
val TURKISH_ORDER: Comparator<String> = Collator.getInstance(TURKISH).let { c -> Comparator { a, b -> c.compare(a, b) } }
private val LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TURKISH)
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)
private val UPDATED = DateTimeFormatter.ofPattern("d MMMM HH:mm", TURKISH)

// "8 Ekim 2026, saat 14:00"; on a card this year's tenders show the weekday instead of the year: "8 Ekim Perşembe".
fun Tender.whenText(long: Boolean = false, withYear: Boolean = true): String? {
    val day = date ?: return null
    val text = day.format(if (long) LONG_DATE else if (withYear) SHORT_DATE else DAY_MONTH)
    return if (time != null) "$text, saat $time" else text
}

// The date and the hour apart, for the detail screen: "2 Ekim 2026, Cuma" and "Saat 11:00".
fun Tender.dayText(): String? = date?.format(LONG_DATE)

fun Tender.hourText(): String? = time?.let { "Saat $it" }

// Days left in plain words: "Bugün", "Yarın", "12 gün kaldı".
fun remainingText(days: Long?): String? = when {
    days == null -> null
    days < 0 -> "Geçti"
    days == 0L -> "Bugün"
    days == 1L -> "Yarın"
    else -> "$days gün kaldı"
}

// Same, but a tender whose hour has already gone today reads "Geçti" rather than "Bugün".
fun Tender.remainingText(now: LocalDateTime = LocalDateTime.now()): String? =
    if (isOver(now)) "Geçti" else remainingText(daysLeft(now.toLocalDate()))

fun LocalDate.shortText(): String = format(SHORT_DATE)

// "2026-10-01T13:30+03:00" from index.json, shown as "1 Ekim 13:30".
fun updatedText(iso: String): String = runCatching { OffsetDateTime.parse(iso).format(UPDATED) }.getOrDefault(iso)
