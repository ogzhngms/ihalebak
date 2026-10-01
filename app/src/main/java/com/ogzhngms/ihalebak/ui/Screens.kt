@file:OptIn(ExperimentalLayoutApi::class)

package com.ogzhngms.ihalebak.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.canNotify
import com.ogzhngms.ihalebak.lowercaseTr
import java.time.LocalDate
import java.time.ZoneId

// EKAP's own search page; a tender has no stable public link, so the İKN is copied for its search box.
private const val EKAP_SEARCH = "https://ekap.kik.gov.tr/EKAP/Ortak/IhaleArama/index.html"
private val ISTANBUL = ZoneId.of("Europe/Istanbul")

@Composable
fun DetailScreen(vm: MainViewModel, tender: Tender, onBack: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val favorite = vm.isFavorite(tender)
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        BackRow("İhale ayrıntısı", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                tender.category?.let { Tag(it.label, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
                if (tender.cancelled) Tag("İptal edildi", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                else daysLeftText(tender.daysLeft(today))?.let { Tag(it, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Text(tender.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field("İhale tarihi", tender.whenText(long = true) ?: "Bültende belirtilmemiş")
                    tender.authority?.let { Field("İdare", it) }
                    tender.province?.let { Field("İl", it) }
                    tender.address?.let { Field("Adres", it) }
                    tender.subject?.let { Field("İşin adı", it) }
                    tender.quantity?.let { Field("Niteliği, türü ve miktarı", it) }
                    Field("İhale kayıt numarası (İKN)", tender.ikn)
                    tender.bulletinDate?.let { Field("Bülten tarihi", it.shortText()) }
                    tender.correctedOn?.let { Field("Düzeltme ilanı", it.shortText()) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.toggleFavorite(tender) }) {
                    Icon(painterResource(if (favorite) R.drawable.ic_star else R.drawable.ic_star_outline), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (favorite) "Favorilerde" else "Favorilere ekle")
                }
                if (tender.dateTime != null && !tender.cancelled) {
                    OutlinedButton(onClick = { addToCalendar(context, tender) }) {
                        Icon(painterResource(R.drawable.ic_event), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Takvime ekle")
                    }
                }
                OutlinedButton(onClick = { share(context, tender) }) {
                    Icon(painterResource(R.drawable.ic_share), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paylaş")
                }
                OutlinedButton(onClick = { copy(context, tender.ikn, "İKN kopyalandı") }) {
                    Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("İKN kopyala")
                }
            }
            Button(onClick = { openOnEkap(context, tender) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_open), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("EKAP'ta aç")
            }
            Disclaimer()
            Spacer(Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun Disclaimer() {
    Text(
        "Bilgiler, Kamu İhale Kurumu'nun yayımladığı Kamu İhale Bülteni'nden otomatik olarak derlenir. " +
            "Kesin bilgi ve ihale dokümanı için EKAP'ı kontrol et.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val today = LocalDate.now()
    val favorites = remember(vm.favorites) { vm.favorites.sortedWith(compareBy<Tender> { it.cancelled }.thenBy { it.dateTime }) }
    if (favorites.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painterResource(R.drawable.ic_star_outline), null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
            Empty("Henüz favori ihalen yok. Bir ihalenin ayrıntısında \"Favorilere ekle\"ye dokun.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(favorites, key = { it.ikn }) { tender -> TenderCard(tender, today, favorite = true) { vm.selected = tender } }
    }
}

// Pick provinces and types to be notified about; the app checks every few hours on its own, no account needed.
@Composable
fun WatchScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(canNotify(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    var query by rememberSaveable { mutableStateOf("") }
    val provinces = vm.index?.provinces.orEmpty()
    val needle = query.trim().lowercaseTr()
    val shown = provinces.filter { needle.isEmpty() || needle in it.name.lowercaseTr() }
        .sortedWith(compareByDescending<com.ogzhngms.ihalebak.ProvinceCount> { it.slug in vm.watchedProvinces }.thenByDescending { it.open })

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Seçtiğin illerde yeni ihale çıkınca haber verelim. Uygulama birkaç saatte bir kendisi kontrol eder; hesap açman gerekmez.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bildirim izni kapalı", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Yeni ihaleleri gösterebilmemiz için bildirimlere izin ver.", color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("İzin ver") }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("İhale türleri", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { category ->
                        FilterChip(selected = category in vm.watchedCategories, onClick = { vm.toggleWatchedCategory(category) }, label = { Text(category.label) })
                    }
                }
                Text(
                    if (vm.watchedCategories.isEmpty()) "Hiçbiri seçili değilse hepsi bildirilir." else "Sadece seçili türler bildirilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "İller" + if (vm.watchedProvinces.isNotEmpty()) " (${vm.watchedProvinces.size} seçili)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SearchField(query, { query = it }, "İl ara")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column {
                    shown.forEachIndexed { i, province ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val watched = province.slug in vm.watchedProvinces
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleWatchedProvince(province.slug) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(province.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${province.open} açık ihale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = watched, onCheckedChange = { vm.toggleWatchedProvince(province.slug) })
                        }
                    }
                }
            }
        }
        item { Disclaimer() }
    }
}

private fun copy(context: Context, text: String, message: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("İKN", text))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun openOnEkap(context: Context, tender: Tender) {
    copy(context, tender.ikn, "İKN kopyalandı, EKAP'taki arama kutusuna yapıştır")
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EKAP_SEARCH)))
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
    val text = listOfNotNull(
        tender.title,
        tender.authority,
        tender.province?.let { "İl: $it" },
        tender.whenText(long = true)?.let { "Tarih: $it" },
        "İKN: ${tender.ikn}",
        "İhaleBak ile paylaşıldı",
    ).joinToString("\n")
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Paylaş"))
}
