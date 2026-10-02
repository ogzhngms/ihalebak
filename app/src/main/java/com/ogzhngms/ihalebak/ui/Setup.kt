@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.ogzhngms.ihalebak.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.canNotify
import com.ogzhngms.ihalebak.searchKey

// The two questions of the first run, also reached from the settings button: which provinces, then which kinds
// of work and whether to announce new tenders. The chosen provinces are both the agenda and what is watched.
@Composable
fun SetupScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val second = vm.setupStep == 2
    BackHandler(enabled = second || vm.cities.isNotEmpty(), onBack = vm::setupBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).height(4.dp).background(palette.accent, RoundedCornerShape(2.dp)))
                Box(Modifier.weight(1f).height(4.dp).background(if (second) palette.accent else palette.line, RoundedCornerShape(2.dp)))
            }
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (second) "Adım 2 / 2" else "Adım 1 / 2", style = type(15), color = palette.muted, modifier = Modifier.weight(1f))
                // Reached from the settings button there is something to go back to; on the first run there is not.
                if (vm.cities.isNotEmpty()) IconTap(R.drawable.ic_close, "Vazgeç", vm::closeSetup)
            }
        }
        Box(Modifier.weight(1f)) { if (second) TypeStep(vm) else CityStep(vm) }
        HorizontalDivider(color = palette.line)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (second) SecondaryButton("Geri", vm::setupBack, Modifier.weight(1f))
            PrimaryButton(
                when {
                    second -> "Ajandamı göster"
                    vm.draftCities.isEmpty() -> "Devam"
                    else -> "Devam (${vm.draftCities.size} şehir)"
                },
                onClick = {
                    if (second && vm.draftNotify && !canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setupNext()
                },
                modifier = Modifier.weight(2f),
                enabled = second || vm.draftCities.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun CityStep(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val index = vm.index
    if (index == null) {
        if (vm.loading) Loading() else Note(R.drawable.ic_calendar_x, "Şehir listesi yüklenemedi. İnternet bağlantını kontrol et.", "Tekrar dene") { vm.load(refresh = true) }
        return
    }
    val needle = query.trim().searchKey()
    val provinces = remember(index, needle) {
        index.provinces.filter { needle.isEmpty() || needle in it.name.searchKey() }.sortedWith(compareBy(TURKISH_ORDER) { it.name })
    }
    // The question and the chosen provinces scroll away with the list; the search box stays in reach.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Hangi şehirlerdeki ihaleleri görmek istersin?", style = heading(32))
                if (vm.draftCities.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.draftCities.forEach { slug ->
                            Surface(onClick = { vm.toggleDraftCity(slug) }, shape = Sharp, color = palette.accent, contentColor = palette.onAccent) {
                                Row(Modifier.heightIn(min = 40.dp).padding(start = 14.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(vm.provinceName(slug) ?: slug, style = type(16))
                                    Icon(painterResource(R.drawable.ic_close), "Çıkar", Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        stickyHeader {
            Box(Modifier.background(palette.bg).padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) { SearchInput(query, { query = it }, "Şehir ara") }
        }
        if (provinces.isEmpty()) item { Text("“${query.trim()}” adında bir şehir bulunamadı.", Modifier.padding(20.dp), style = type(17), color = palette.muted) }
        items(provinces, key = { it.slug }) { province ->
            val on = province.slug in vm.draftCities
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(Sharp).clickable { vm.toggleDraftCity(province.slug) }.heightIn(min = 54.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(26.dp).then(if (on) Modifier.background(palette.accent, Sharp) else Modifier.border(1.5.dp, palette.faint, Sharp)),
                    contentAlignment = Alignment.Center,
                ) { if (on) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(17.dp), tint = palette.onAccent) }
                Text(province.name, style = type(19), modifier = Modifier.weight(1f))
                Text(if (province.open == 0) "yok" else "${province.open} ihale", style = type(15).copy(fontFeatureSettings = "tnum"), color = palette.muted)
            }
        }
    }
}

private fun Category.hint() = when (this) {
    Category.GOODS -> "Yakıt, malzeme, cihaz, gıda"
    Category.SERVICES -> "Temizlik, yemek, güvenlik, taşıma"
    Category.WORKS -> "Bina, yol, altyapı, onarım"
    Category.CONSULTANCY -> "Proje, etüt, müşavirlik"
}

private fun Category.icon() = when (this) {
    Category.GOODS -> R.drawable.ic_package
    Category.SERVICES -> R.drawable.ic_briefcase
    Category.WORKS -> R.drawable.ic_hardhat
    Category.CONSULTANCY -> R.drawable.ic_compass
}

@Composable
private fun TypeStep(vm: MainViewModel) {
    // Nothing chosen means everything, so every row shows as on in that case.
    val chosen = vm.draftCategories.ifEmpty { Category.entries.toSet() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
        Text("Ne tür işlerle ilgileniyorsun?", style = heading(32))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Category.entries.forEach { category ->
                val on = category in chosen
                Surface(
                    onClick = { vm.toggleDraftCategory(category) },
                    shape = Sharp,
                    color = if (on) palette.accentSoft else palette.surface,
                    contentColor = if (on) palette.onAccentSoft else palette.text,
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(category.icon()), null, Modifier.size(28.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(category.label, style = type(19, FontWeight.SemiBold, 1.25f))
                            Text(category.hint(), style = type(15), modifier = Modifier.alpha(0.85f))
                        }
                        Icon(painterResource(if (on) R.drawable.ic_check_circle else R.drawable.ic_circle), if (on) "Seçili" else "Seçili değil", Modifier.size(24.dp))
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(Sharp).clickable { vm.draftNotify = !vm.draftNotify }.heightIn(min = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_bell), null, Modifier.size(28.dp), tint = palette.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Yeni ihale çıkınca haber ver", style = type(19, FontWeight.SemiBold, 1.25f))
                Text("Hesap gerekmez, günde birkaç kez bakılır.", style = type(15), color = palette.muted)
            }
            Switch(
                checked = vm.draftNotify,
                onCheckedChange = { vm.draftNotify = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = palette.accent, checkedThumbColor = palette.bg,
                    uncheckedTrackColor = palette.handle, uncheckedThumbColor = palette.bg, uncheckedBorderColor = palette.handle,
                ),
            )
        }
    }
}
