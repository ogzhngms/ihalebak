@file:OptIn(ExperimentalMaterial3Api::class)

package com.ogzhngms.ihalebak.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tender
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

// EKAP's tender search. A tender has no public link of its own, and the search asks for the İKN in two boxes
// (year and number), so the app shows both before opening it.
private const val EKAP_SEARCH = "https://ekapv2.kik.gov.tr/ekap/search"
private val ISTANBUL = ZoneId.of("Europe/Istanbul")

// A tender, as a sheet over the list: what and where, the date as a calendar leaf, the facts, and along the
// bottom the way to EKAP beside favourite, calendar and share.
@Composable
fun DetailSheet(vm: MainViewModel, tender: Tender) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = LocalDateTime.now()
    val favorite = vm.isFavorite(tender)
    var explaining by rememberSaveable { mutableStateOf(false) }
    fun close() {
        scope.launch { sheet.hide() }.invokeOnCompletion { vm.selected = null }
    }
    if (explaining) EkapHelp(tender, onOpen = { explaining = false; openOnEkap(context, tender) }, onDismiss = { explaining = false })

    ModalBottomSheet(
        onDismissRequest = { vm.selected = null },
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        containerColor = palette.bg,
        contentColor = palette.text,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(start = 44.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(44.dp, 5.dp).background(palette.handle, RoundedCornerShape(3.dp)))
                }
                IconTap(R.drawable.ic_close, "Kapat", ::close)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listOfNotNull(tender.category?.label ?: "İhale", tender.province).joinToString(" · "), style = type(15), color = palette.muted)
                    Text(tender.title, style = heading(28, 1.2f))
                }
                DateBox(tender, now)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    tender.authority?.let { Fact("Kurum", it) }
                    tender.subject?.let { Fact("İşin adı", it) }
                    tender.quantity?.let { Fact("Miktarı", it) }
                    tender.address?.let { Fact("Adres", it) }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("İhale kayıt numarası (İKN)", style = type(15), color = palette.muted)
                            Text(tender.ikn, style = type(21, FontWeight.SemiBold, 1.3f).copy(fontFeatureSettings = "tnum"))
                        }
                        GhostButton("Kopyala", { copy(context, tender.ikn); Toast.makeText(context, "Numara kopyalandı", Toast.LENGTH_SHORT).show() }, icon = R.drawable.ic_copy)
                    }
                }
            }
            HorizontalDivider(color = palette.line)
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("EKAP'ta aç", { explaining = true }, Modifier.weight(1f), icon = R.drawable.ic_open)
                SquareButton(
                    if (favorite) R.drawable.ic_star_on else R.drawable.ic_star,
                    if (favorite) "Favorilerden çıkar" else "Favorilere ekle",
                    onClick = {
                        Toast.makeText(context, if (favorite) "Favorilerden çıkarıldı" else "Favorilere eklendi", Toast.LENGTH_SHORT).show()
                        vm.toggleFavorite(tender)
                    },
                    tint = if (favorite) palette.star else palette.text,
                )
                SquareButton(R.drawable.ic_calendar_plus, "Takvime ekle", { addToCalendar(context, tender) }, enabled = !tender.cancelled && tender.dateTime != null)
                SquareButton(R.drawable.ic_share, "Paylaş", { share(context, tender) })
            }
        }
    }
}

// The date as a small calendar leaf (month over day) beside the weekday, the hour and how long is left.
@Composable
private fun DateBox(tender: Tender, now: LocalDateTime) {
    val day = tender.date
    Row(
        Modifier.fillMaxWidth().clip(Sharp).background(if (tender.cancelled) palette.urgentSoft else palette.accentSoft).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (day != null) {
            Surface(shape = Sharp, color = palette.bg, contentColor = palette.text, shadowElevation = 1.dp) {
                Column(Modifier.widthIn(min = 64.dp).padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.monthName(), style = type(13, FontWeight.SemiBold, 1.3f), color = palette.urgent)
                    Text(day.dayOfMonth.toString(), style = heading(30, 1.05f))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tender.weekdayTime() ?: "Tarih bültende belirtilmemiş", style = type(19, FontWeight.SemiBold, 1.3f))
            tender.remainText(now)?.let { Text(it, style = type(17, FontWeight.SemiBold, 1.3f), color = if (tender.isUrgent(now)) palette.urgent else palette.accentText) }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = type(15), color = palette.muted)
        Text(value, style = type(18))
    }
}

@Composable
private fun EkapHelp(tender: Tender, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val (year, number) = tender.iknParts()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(4.dp), color = palette.bg, contentColor = palette.text, shadowElevation = 12.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EKAP'ta bu numarayı ara", style = heading(24, 1.2f))
                Column(Modifier.fillMaxWidth().background(palette.accentSoft, Sharp).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Yıl" to year, "Numara" to number).forEach { (label, value) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(label, style = type(15), color = palette.onAccentSoft, modifier = Modifier.widthIn(min = 64.dp).alignByBaseline())
                            Text(value, style = heading(30).copy(fontFeatureSettings = "tnum"), color = palette.onAccentSoft, modifier = Modifier.alignByBaseline())
                        }
                    }
                }
                Text("Numara panoya kopyalanır. EKAP'ın arama sayfasında “İKN” kutularına yılı yaz, numarayı yapıştır.", style = type(16, lineHeight = 1.45f), color = palette.muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    GhostButton("Vazgeç", onDismiss)
                    PrimaryButton("EKAP'ı aç", onOpen, minHeight = 48.dp, size = 16)
                }
            }
        }
    }
}

// "2026/1781203" as its year and its number, the two boxes EKAP's search asks for.
private fun Tender.iknParts(): Pair<String, String> = ikn.substringBefore('/') to ikn.substringAfter('/', "")

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("İKN", text))
}

private fun openOnEkap(context: Context, tender: Tender) {
    copy(context, tender.iknParts().second.ifEmpty { tender.ikn })
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EKAP_SEARCH))) }
        .onFailure { Toast.makeText(context, "Tarayıcı bulunamadı", Toast.LENGTH_SHORT).show() }
}

private fun addToCalendar(context: Context, tender: Tender) {
    val start = tender.dateTime?.atZone(ISTANBUL)?.toInstant()?.toEpochMilli() ?: return
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000)
        .putExtra(CalendarContract.Events.TITLE, "İhale: ${tender.title}")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, tender.address ?: tender.province ?: "")
        .putExtra(CalendarContract.Events.DESCRIPTION, listOfNotNull(tender.authority, "İKN: ${tender.ikn}").joinToString("\n"))
    runCatching { context.startActivity(intent) }.onFailure { Toast.makeText(context, "Takvim uygulaması bulunamadı", Toast.LENGTH_SHORT).show() }
}

private fun share(context: Context, tender: Tender) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tender.shareText())
    context.startActivity(Intent.createChooser(send, "Paylaş"))
}
