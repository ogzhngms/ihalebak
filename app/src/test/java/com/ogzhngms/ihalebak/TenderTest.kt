package com.ogzhngms.ihalebak

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TenderTest {
    private val today = LocalDate.of(2026, 10, 1)
    private val now = today.atTime(9, 0)

    private fun tender(ikn: String, date: String?, category: String = "mal", status: String = "open", title: String = "AKARYAKIT SATIN ALINACAKTIR") =
        Tender.parse(
            JSONObject()
                .put("ikn", ikn)
                .put("title", title)
                .put("category", category)
                .put("status", status)
                .put("authority", "İzmir Büyükşehir Belediyesi")
                .apply { if (date != null) put("date", date).put("time", "10:00") },
        )

    @Test
    fun readsAPublishedRecord() {
        val t = Tender.parseList(JSONArray().put(JSONObject("""{"ikn":"2026/1783854","title":"AKARYAKIT SATIN ALINACAKTIR","category":"yapim","status":"cancelled","date":"2026-10-22","time":"10:00","province":"İzmir"}"""))).single()
        assertEquals("Akaryakıt Satın Alınacaktır", t.title)
        assertEquals(Category.WORKS, t.category)
        assertTrue(t.cancelled)
        assertEquals(21L, t.daysLeft(today))
        assertNull(t.subject)
    }

    @Test
    fun titleCaseIsTurkish() {
        assertEquals("İnşaat Işleri", "İNŞAAT IŞLERİ".toTitleCaseTr())
        assertEquals("Zaten Düzgün", "Zaten Düzgün".toTitleCaseTr())
    }

    @Test
    fun filterSearchesSortsAndDropsPastTenders() {
        val list = listOf(
            tender("1", "2026-10-20"),
            tender("2", "2026-10-05", category = "hizmet"),
            tender("3", "2026-09-30"),
            tender("4", "2026-10-03", status = "cancelled"),
            tender("5", null, title = "KÖPRÜ YAPIM İŞİ"),
        )
        assertEquals(listOf("2", "1", "5", "4"), list.filtered("", emptySet(), now).map { it.ikn })
        assertEquals(listOf("2"), list.filtered("", setOf(Category.SERVICES), now).map { it.ikn })
        assertEquals(listOf("5"), list.filtered("köprü", emptySet(), now).map { it.ikn })
        // Search ignores case the Turkish way and also looks at the institution.
        assertEquals(listOf("2", "1", "5", "4"), list.filtered("İZMİR", emptySet(), now).map { it.ikn })
    }

    @Test
    fun aTenderIsOverOnceItsHourHasPassedToday() {
        val tenAm = tender("1", "2026-10-01")
        assertEquals(false, tenAm.isOver(today.atTime(9, 59)))
        assertEquals(true, tenAm.isOver(today.atTime(10, 1)))
        assertEquals(false, tender("2", null).isOver(today.atTime(23, 0)))
    }

    @Test
    fun onlyNewOpenTendersOfWatchedTypesAreAnnounced() {
        val current = listOf(tender("1", "2026-10-20"), tender("2", "2026-10-21", category = "yapim"), tender("3", "2026-10-22", status = "cancelled"))
        assertEquals(emptyList<Tender>(), newTenders(current, seen = null, categories = emptySet()))
        assertEquals(listOf("2"), newTenders(current, seen = setOf("1"), categories = emptySet()).map { it.ikn })
        assertEquals(emptyList<String>(), newTenders(current, seen = setOf("1"), categories = setOf(Category.GOODS)).map { it.ikn })
    }

    @Test
    fun capitalsAreShownAsTitleCaseButAbbreviationsStay() {
        assertEquals("İzmir Büyükşehir Belediye Başkanlığı", "İZMİR BÜYÜKŞEHİR BELEDİYE BAŞKANLIĞI".toTitleCaseTr())
        assertEquals("TCDD Bakım-Onarım ve Temizlik Hizmeti", "TCDD BAKIM-ONARIM VE TEMİZLİK HİZMETİ".toTitleCaseTr())
        assertEquals("DSİ 21. Bölge Müdürlüğü", "DSİ 21. BÖLGE MÜDÜRLÜĞÜ".toTitleCaseTr())
        assertEquals("Konak Belediyesi KGM", "Konak Belediyesi KGM".toTitleCaseTr())
    }
}
