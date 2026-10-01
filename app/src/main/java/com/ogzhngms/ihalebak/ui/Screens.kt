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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.canNotify
import java.time.LocalDate
import java.time.ZoneId

// EKAP's own search page; a tender has no stable public link, so the İKN is copied for its search box.
private const val EKAP_HOME = "https://ekap.kik.gov.tr"
private const val EKAP_SEARCH = "https://ekap.kik.gov.tr/EKAP/Ortak/IhaleArama/index.html"
private val ISTANBUL = ZoneId.of("Europe/Istanbul")

// The date first and large, then the facts with plain labels, then one action per full-width button.
@Composable
fun DetailScreen(vm: MainViewModel, tender: Tender, onBack: () -> Unit) {
    val context = LocalContext.current
    val saved = vm.isFavorite(tender)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        BackRow("İhale", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tender.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Surface(color = if (tender.cancelled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("İhale tarihi", style = MaterialTheme.typography.labelLarge)
                    Text(tender.dayText() ?: "Bültende belirtilmemiş", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    tender.hourText()?.let { Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    val note = if (tender.cancelled) "Bu ihale iptal edildi." else tender.remainingText()
                    if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    tender.authority?.let { Field("Kurum", it) }
                    tender.province?.let { Field("Şehir", it) }
                    tender.category?.let { Field("Türü", it.label) }
                    tender.subject?.let { Field("İşin adı", it) }
                    tender.quantity?.let { Field("Miktarı", it) }
                    tender.address?.let { Field("Adres", it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Field("İhale kayıt numarası", tender.ikn) }
                        TextButton(onClick = { copy(context, tender.ikn, "Numara kopyalandı") }) { Text("Kopyala") }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { openOnEkap(context, tender) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(painterResource(R.drawable.ic_open), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("EKAP'ta aç", style = MaterialTheme.typography.titleSmall)
                }
                BigOutlined(if (saved) "Kaydedildi" else "Kaydet", if (saved) R.drawable.ic_star else R.drawable.ic_star_outline) { vm.toggleFavorite(tender) }
                if (tender.dateTime != null && !tender.cancelled) {
                    BigOutlined("Takvime ekle", R.drawable.ic_event) { addToCalendar(context, tender) }
                }
                BigOutlined("Paylaş", R.drawable.ic_share) { share(context, tender) }
            }
            Disclaimer()
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun BigOutlined(text: String, icon: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(painterResource(icon), null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// Google Play asks apps that show government information to say plainly that they are not official and to
// name their source; this says both, on every screen that lists tenders.
@Composable
fun Disclaimer(modifier: Modifier = Modifier) {
    val link = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("İhaleBak resmi bir devlet uygulaması değildir; Kamu İhale Kurumu veya EKAP ile bağlantısı yoktur. ")
        }
        append("Bilgiler, Kamu İhale Kurumu'nun herkese açık Kamu İhale Bülteni'nden otomatik derlenir ve hata içerebilir. Kesin bilgi için: ")
        withLink(LinkAnnotation.Url(EKAP_HOME, link)) { append("ekap.kik.gov.tr") }
    }
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val today = LocalDate.now()
    val favorites = remember(vm.favorites) { vm.favorites.sortedWith(compareBy<Tender> { it.cancelled }.thenBy { it.dateTime }) }
    if (favorites.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painterResource(R.drawable.ic_star_outline), null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
            Empty("Henüz kaydettiğin ihale yok.\nBir ihaleyi açıp \"Kaydet\"e dokunursan burada görünür.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Kaydettiğin ihaleler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(favorites, key = { it.ikn }) { tender -> TenderCard(tender, today) { vm.selected = tender } }
    }
}

// Only the provinces being watched are listed, with one clear button to add another.
@Composable
fun WatchScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(canNotify(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    var adding by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = adding) { adding = false }
    if (adding) {
        ProvincePicker(
            vm,
            title = "Hangi şehri takip edelim?",
            onPick = { slug ->
                if (slug !in vm.watchedProvinces) vm.toggleWatchedProvince(slug)
                adding = false
                if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onCancel = { adding = false },
        )
        return
    }
    val watched = vm.index?.provinces.orEmpty().filter { it.slug in vm.watchedProvinces }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Yeni ihale bildirimleri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Takip ettiğin şehirlerde yeni ihale çıkınca telefonuna bildirim gelir. Hesap açman gerekmez.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && watched.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bildirimler kapalı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Haber verebilmemiz için bildirimlere izin ver.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.heightIn(min = 52.dp)) { Text("İzin ver") }
                }
            }
        }

        Text("Takip ettiğin şehirler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(14.dp)) {
            Column {
                if (watched.isEmpty()) {
                    Text("Henüz şehir eklemedin.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
                watched.forEach { province ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(province.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.toggleWatchedProvince(province.slug) }) { Text("Kaldır") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                TextButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("+ Şehir ekle", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        Text("Hangi ihaleler?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(14.dp)) {
            Column {
                // Nothing chosen means everything, so show every box ticked in that case.
                val chosen = vm.watchedCategories.ifEmpty { Category.entries.toSet() }
                Category.entries.forEachIndexed { i, category ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val on = category in chosen
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { toggleKind(vm, chosen, category) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = on, onCheckedChange = { toggleKind(vm, chosen, category) })
                        Text(category.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        vm.index?.updatedAt?.let {
            Text("Bilgiler en son ${updatedText(it)} tarihinde güncellendi.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Disclaimer()
    }
}

// Keeps at least one type chosen, and stores "all four" as the empty set, which means every type.
private fun toggleKind(vm: MainViewModel, chosen: Set<Category>, category: Category) {
    val next = if (category in chosen) chosen - category else chosen + category
    if (next.isEmpty()) return
    val stored = if (next.size == Category.entries.size) emptySet() else next
    Category.entries.filter { (it in stored) != (it in vm.watchedCategories) }.forEach { vm.toggleWatchedCategory(it) }
}

private fun copy(context: Context, text: String, message: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("İKN", text))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun openOnEkap(context: Context, tender: Tender) {
    copy(context, tender.ikn, "İhale numarası kopyalandı. EKAP'ta arama kutusuna yapıştırın.")
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
        tender.province?.let { "Şehir: $it" },
        tender.whenText(long = true)?.let { "Tarih: $it" },
        "İhale kayıt numarası: ${tender.ikn}",
        "İhaleBak ile paylaşıldı",
    ).joinToString("\n")
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Paylaş"))
}
