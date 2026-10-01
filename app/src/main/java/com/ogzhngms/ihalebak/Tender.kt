package com.ogzhngms.ihalebak

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import org.json.JSONArray
import org.json.JSONObject

// The four kinds of public procurement the bulletin publishes, under the ids the collector writes.
enum class Category(val id: String, val label: String) {
    GOODS("mal", "Mal alımı"),
    SERVICES("hizmet", "Hizmet alımı"),
    WORKS("yapim", "Yapım işi"),
    CONSULTANCY("danismanlik", "Danışmanlık"),
    ;

    companion object {
        fun of(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}

// One notice from the Kamu İhale Bülteni, as published by the collector (collector/build.py).
data class Tender(
    val ikn: String,
    val title: String,
    val category: Category?,
    val cancelled: Boolean,
    val authority: String?,
    val address: String?,
    val province: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val subject: String?,
    val quantity: String?,
    val bulletinDate: LocalDate?,
    val correctedOn: LocalDate?,
    val json: String,
) {
    val dateTime: LocalDateTime? get() = date?.atTime(time ?: LocalTime.of(9, 0))

    // Days from today to the tender: 0 is today, negative has passed, null when the bulletin gave no date.
    fun daysLeft(today: LocalDate): Long? = date?.let { ChronoUnit.DAYS.between(today, it) }

    // Title, subject, institution and İKN, lower-cased the Turkish way, for search.
    val searchText: String by lazy { listOfNotNull(title, subject, authority, ikn).joinToString(" ").lowercaseTr() }

    companion object {
        fun parse(json: JSONObject): Tender = Tender(
            ikn = json.getString("ikn"),
            title = json.optString("title").ifBlank { json.getString("ikn") }.toTitleCaseTr(),
            category = Category.of(json.optString("category")),
            cancelled = json.optString("status") == "cancelled",
            authority = json.optStringOrNull("authority"),
            address = json.optStringOrNull("address"),
            province = json.optStringOrNull("province"),
            date = json.optStringOrNull("date")?.let(LocalDate::parse),
            time = json.optStringOrNull("time")?.let(LocalTime::parse),
            subject = json.optStringOrNull("subject"),
            quantity = json.optStringOrNull("quantity"),
            bulletinDate = json.optStringOrNull("bulletin_date")?.let(LocalDate::parse),
            correctedOn = json.optStringOrNull("corrected_on")?.let(LocalDate::parse),
            json = json.toString(),
        )

        fun parseList(array: JSONArray): List<Tender> = List(array.length()) { parse(array.getJSONObject(it)) }
    }
}

// A province with how many open tenders it has, from index.json.
data class ProvinceCount(val name: String, val slug: String, val open: Int)

data class Index(val updatedAt: String, val provinces: List<ProvinceCount>) {
    companion object {
        fun parse(json: JSONObject): Index {
            val provinces = json.getJSONArray("provinces")
            return Index(
                updatedAt = json.getString("updated_at"),
                provinces = List(provinces.length()) {
                    val p = provinces.getJSONObject(it)
                    ProvinceCount(p.getString("name"), p.getString("slug"), p.getInt("open"))
                },
            )
        }
    }
}

// Open tenders first, soonest first; cancelled ones at the end. Past tenders are dropped by the collector.
fun List<Tender>.filtered(query: String, categories: Set<Category>, today: LocalDate): List<Tender> {
    val needle = query.trim().lowercaseTr()
    return filter { (categories.isEmpty() || it.category in categories) && (needle.isEmpty() || needle in it.searchText) }
        .filter { (it.daysLeft(today) ?: 0) >= 0 }
        .sortedWith(compareBy<Tender> { it.cancelled }.thenBy { it.dateTime ?: LocalDateTime.MAX })
}

private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) optString(name).ifBlank { null } else null

private val TURKISH = java.util.Locale.forLanguageTag("tr")

fun String.lowercaseTr(): String = lowercase(TURKISH)

// Bulletin titles are in capitals ("AKARYAKIT SATIN ALINACAKTIR"); show them as "Akaryakıt Satın Alınacaktır".
fun String.toTitleCaseTr(): String {
    if (any { it.isLowerCase() }) return this
    return lowercase(TURKISH).split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(TURKISH) }
    }
}
