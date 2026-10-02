package com.ogzhngms.ihalebak.ui

import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.isOver
import java.text.Collator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")

// Alphabetical the Turkish way: Ç after C, İ after I, Ş after S.
val TURKISH_ORDER: Comparator<String> = Collator.getInstance(TURKISH).let { c -> Comparator { a, b -> c.compare(a, b) } }

private val WEEKDAY = DateTimeFormatter.ofPattern("EEEE", TURKISH)
private val WEEKDAY_SHORT = DateTimeFormatter.ofPattern("EEE", TURKISH)
private val MONTH = DateTimeFormatter.ofPattern("MMMM", TURKISH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", TURKISH)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH)
private val HOUR = DateTimeFormatter.ofPattern("HH:mm", TURKISH)

// A day's heading in the agenda: "Bugün", "Yarın", then the weekday's name.
fun dayTitle(day: LocalDate, today: LocalDate): String = when (ChronoUnit.DAYS.between(today, day)) {
    0L -> "Bugün"
    1L -> "Yarın"
    else -> day.format(WEEKDAY)
}

// "5 Ekim", with the year once it is not this one.
fun dayMonth(day: LocalDate, today: LocalDate): String = day.format(if (day.year == today.year) DAY_MONTH else DAY_MONTH_YEAR)

fun LocalDate.weekdayShort(): String = format(WEEKDAY_SHORT)

fun LocalDate.monthName(): String = format(MONTH)

// "Cuma, saat 15:30" under the date on a tender's sheet.
fun Tender.weekdayTime(): String? = date?.let { day -> day.format(WEEKDAY) + (time?.let { ", saat $it" } ?: "") }

// How long is left, in plain words: "Bugün", "Yarın", "12 gün kaldı".
fun Tender.remainText(now: LocalDateTime): String? = when {
    cancelled -> "Bu ihale iptal edildi"
    isOver(now) -> "Tarihi geçti"
    else -> when (val days = daysLeft(now.toLocalDate())) {
        null -> null
        0L -> "Bugün"
        1L -> "Yarın"
        else -> "$days gün kaldı"
    }
}

// Cancelled, today or tomorrow: shown in the urgent colour.
fun Tender.isUrgent(now: LocalDateTime): Boolean = cancelled || (!isOver(now) && (daysLeft(now.toLocalDate()) ?: 99) <= 1)

// "İzmir", "İzmir ve Manisa", "İzmir, Manisa ve Aydın".
fun listJoin(names: List<String>): String = if (names.size <= 1) names.firstOrNull().orEmpty() else names.dropLast(1).joinToString(", ") + " ve " + names.last()

// When the data was last collected, from index.json's "2026-10-02T13:30+03:00": "bugün 13:30", "dün 13:30", "30 Eylül 13:30".
fun updatedText(iso: String, today: LocalDate): String {
    val at = runCatching { OffsetDateTime.parse(iso) }.getOrNull() ?: return iso
    val day = when (ChronoUnit.DAYS.between(at.toLocalDate(), today)) {
        0L -> "bugün"
        1L -> "dün"
        else -> dayMonth(at.toLocalDate(), today)
    }
    return "$day ${at.format(HOUR)}"
}

fun Tender.shareText(): String = listOfNotNull(
    title,
    authority,
    province?.let { "Şehir: $it" },
    date?.let { "Tarih: ${it.format(DAY_MONTH_YEAR)}" + (time?.let { hour -> ", saat $hour" } ?: "") },
    "İhale kayıt numarası: $ikn",
    "İhaleBak ile paylaşıldı",
).joinToString("\n")
