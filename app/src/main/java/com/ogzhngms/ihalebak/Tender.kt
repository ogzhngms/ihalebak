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

    // Title, subject, institution and İKN, folded like the query, for search.
    val searchText: String by lazy { listOfNotNull(title, subject, authority, ikn).joinToString(" ").searchKey() }

    companion object {
        fun parse(json: JSONObject): Tender = Tender(
            ikn = json.getString("ikn"),
            title = json.optString("title").ifBlank { json.getString("ikn") }.toTitleCaseTr(),
            category = Category.of(json.optString("category")),
            cancelled = json.optString("status") == "cancelled",
            authority = json.optStringOrNull("authority")?.toTitleCaseTr(),
            address = json.optStringOrNull("address")?.toTitleCaseTr(),
            province = json.optStringOrNull("province"),
            date = json.optStringOrNull("date")?.let(LocalDate::parse),
            time = json.optStringOrNull("time")?.let(LocalTime::parse),
            subject = json.optStringOrNull("subject")?.toTitleCaseTr(),
            quantity = json.optStringOrNull("quantity")?.toTitleCaseTr(),
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

// A tender is over once its day has passed, or its hour has on the day itself. Undated ones stay until the collector drops them.
fun Tender.isOver(now: LocalDateTime): Boolean {
    val day = date ?: return false
    return day < now.toLocalDate() || (day == now.toLocalDate() && time != null && time < now.toLocalTime())
}

// Tenders that match the search and the chosen types, soonest first, undated ones last. Tenders that are over
// are left out; cancelled ones stay in their place in the calendar.
fun List<Tender>.filtered(query: String, categories: Set<Category>, now: LocalDateTime): List<Tender> {
    val needle = query.trim().searchKey()
    return filter { (categories.isEmpty() || it.category in categories) && (needle.isEmpty() || needle in it.searchText) }
        .filterNot { it.isOver(now) }
        .chronological()
}

fun List<Tender>.chronological(): List<Tender> = sortedWith(compareBy { it.dateTime ?: LocalDateTime.MAX })

// The agenda's sections: one per day in order, with the undated tenders (null) at the end.
fun List<Tender>.byDay(): List<Pair<LocalDate?, List<Tender>>> = chronological().groupBy { it.date }.toList()

private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) optString(name).ifBlank { null } else null

private val TURKISH = java.util.Locale.forLanguageTag("tr")

fun String.lowercaseTr(): String = lowercase(TURKISH)

// Lower case without Turkish letters, so "insaat" finds "İnşaat" and "eskisehir" finds "Eskişehir": many people
// type without ç, ğ, ı, ö, ş and ü.
fun String.searchKey(): String = lowercaseTr().map { PLAIN[it] ?: it }.joinToString("")

private val PLAIN = mapOf('ç' to 'c', 'ğ' to 'g', 'ı' to 'i', 'ö' to 'o', 'ş' to 's', 'ü' to 'u', 'â' to 'a', 'î' to 'i', 'û' to 'u')

// Bulletin text is often in capitals ("İZMİR BÜYÜKŞEHİR BELEDİYE BAŞKANLIĞI"), which reads as shouting; show it as
// "İzmir Büyükşehir Belediye Başkanlığı". Text that is mostly lower case already is left alone, and abbreviations
// (TCDD, DSİ, A.Ş.) keep their capitals.
fun String.toTitleCaseTr(): String {
    val letters = filter { it.isLetter() }
    if (letters.isEmpty() || letters.count { it.isUpperCase() } < letters.length * 0.6) return this
    return split(" ").mapIndexed { i, word -> word.casedTr(first = i == 0) }.joinToString(" ")
}

private const val VOWELS = "AEIİOÖUÜ"
private val ABBREVIATIONS = setOf(
    "TC", "AŞ", "TOKİ", "DSİ", "DHMİ", "TEİAŞ", "BOTAŞ", "EÜAŞ", "TEDAŞ", "TÜBİTAK", "TPAO", "TMO", "MEB", "AFAD", "OSB",
    "İSKİ", "ASKİ", "İZSU", "İETT", "BUSKİ", "ESHOT", "TİGEM", "ETİ", "TKİ", "KİT", "MAPEG", "TÜİK", "SGK", "TSK", "EGO",
)
private val SMALL_WORDS = setOf("ve", "ile", "veya", "ya", "da", "de", "ki", "için")

private fun String.casedTr(first: Boolean): String {
    if (any { it.isLowerCase() }) return this
    val core = filter { it.isLetter() }
    if (core.isEmpty() || core in ABBREVIATIONS || (core.length >= 2 && core.none { it in VOWELS })) return this
    val lower = lowercase(TURKISH)
    if (!first && lower in SMALL_WORDS) return lower
    // Capital after a hyphen, slash, bracket or full stop too: "Bakım-Onarım", "Merkez/İzmir", "Ltd.Şti.".
    return buildString {
        lower.forEachIndexed { i, c -> append(if (i == 0 || lower[i - 1] in "-/(.\"") c.titlecase(TURKISH) else c.toString()) }
    }
}
