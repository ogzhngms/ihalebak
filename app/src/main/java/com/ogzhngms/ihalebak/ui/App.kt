@file:OptIn(ExperimentalMaterial3Api::class)

package com.ogzhngms.ihalebak.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.View
import com.ogzhngms.ihalebak.byDay
import com.ogzhngms.ihalebak.filtered
import java.time.LocalDate
import java.time.LocalDateTime

private const val STRIP_DAYS = 14
// Past this many chosen provinces their names are counted, not listed.
private const val MANY_CITIES = 4
// Past this many, a chip for each would be a row nobody scrolls through.
private const val MOST_CHIPS = 12

// No province list and no tabs along the bottom: two questions on the first run, then one agenda. A tender opens
// as a sheet over the list, so the list is never left.
@Composable
fun App(vm: MainViewModel) {
    Surface(Modifier.fillMaxSize(), color = palette.bg, contentColor = palette.text) {
        if (vm.setupStep > 0) SetupScreen(vm) else MainScreen(vm)
    }
    vm.selected?.let { DetailSheet(vm, it) }
}

@Composable
private fun MainScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.searching || vm.day != null || vm.view == View.SAVED) {
        when {
            vm.searching -> vm.toggleSearch()
            vm.day != null -> vm.day = null
            else -> vm.view = View.AGENDA
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("İhaleBak", style = type(26, FontWeight.Bold, 1.2f).copy(letterSpacing = (-0.025).em), modifier = Modifier.weight(1f))
                IconTap(R.drawable.ic_search, "Ara", vm::toggleSearch, if (vm.searching) palette.accentText else palette.text)
                IconTap(R.drawable.ic_sliders, "Şehir ve iş türü seçimi", vm::openSetup)
            }
            Row(
                Modifier.fillMaxWidth().padding(end = 8.dp).clip(RoundedCornerShape(10.dp)).background(palette.well).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Segment(R.drawable.ic_calendar, "Ajanda", vm.view == View.AGENDA, Modifier.weight(1f)) { vm.view = View.AGENDA }
                Segment(R.drawable.ic_star, "Favoriler (${vm.favorites.size})", vm.view == View.SAVED, Modifier.weight(1f)) { vm.view = View.SAVED }
            }
        }
        if (vm.searching) {
            SearchInput(vm.query, { vm.query = it }, "İş, kurum veya İKN ara", Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp), focus = true)
        }
        when (vm.view) {
            View.AGENDA -> Agenda(vm)
            View.SAVED -> Saved(vm)
        }
    }
}

@Composable
private fun Segment(icon: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) palette.bg else Color.Transparent,
        contentColor = if (selected) palette.text else palette.muted,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(Modifier.heightIn(min = 42.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, style = type(16, FontWeight.SemiBold), maxLines = 1)
        }
    }
}

// The chosen provinces' tenders in day and hour order, under a headline that counts them, a row of province
// chips and a two-week strip of days.
@Composable
private fun Agenda(vm: MainViewModel) {
    val today = LocalDate.now()
    val filterName = vm.provinceName(vm.cityFilter)
    val base = remember(vm.tenders, vm.query, vm.categories, filterName) {
        vm.tenders.filtered(vm.query, vm.categories, LocalDateTime.now()).filter { filterName == null || it.province == filterName }
    }
    val list = remember(base, vm.day) { if (vm.day == null) base else base.filter { it.date == vm.day } }
    val groups = remember(list) { list.byDay() }
    val cities = filterName?.let(::listOf) ?: vm.cities.mapNotNull(vm::provinceName)
    // A long list of names would fill the screen: every province is "Tüm şehirler", many are counted.
    val where = when {
        filterName == null && vm.isEveryProvince(vm.cities) -> "Tüm şehirler"
        cities.size > MANY_CITIES -> "${cities.size} şehir"
        else -> listJoin(cities)
    }
    val day = vm.day
    val waiting = vm.tenders.isEmpty() && vm.loading
    val failed = vm.tenders.isEmpty() && vm.failed

    val headline = when {
        waiting -> "Yükleniyor"
        day != null -> "${list.size} ihale"
        vm.query.isNotBlank() -> "${list.size} sonuç"
        else -> "${base.count { !it.cancelled }} açık ihale"
    }
    val types = if (vm.categories.isEmpty()) "tüm işler" else Category.entries.filter { it in vm.categories }.joinToString(", ") { it.label }
    val subline = if (day != null) "${dayTitle(day, today)}, ${dayMonth(day, today)} · $where" else "$where · $types"

    PullToRefreshBox(isRefreshing = vm.loading && vm.tenders.isNotEmpty(), onRefresh = { vm.load(refresh = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { Headline(headline, subline) }
            item { CityChips(vm) }
            item { DayStrip(today, vm.day, base) { vm.day = if (vm.day == it) null else it } }
            if (day != null) item { GhostButton("Tüm günleri göster", { vm.day = null }, Modifier.padding(start = 14.dp, top = 4.dp), R.drawable.ic_close) }
            if (vm.offline && !failed) {
                item { Text("İnternet yok, son indirilen bilgiler gösteriliyor.", Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), style = type(15), color = palette.urgent) }
            }
            when {
                waiting -> item { Loading() }
                failed -> item { Note(R.drawable.ic_calendar_x, "İhaleler yüklenemedi. İnternet bağlantını kontrol et.", "Tekrar dene") { vm.load(refresh = true) } }
                list.isEmpty() -> item {
                    val text = when {
                        day != null -> "${dayTitle(day, today)} için ihale yok."
                        vm.query.isNotBlank() -> "“${vm.query.trim()}” ile eşleşen ihale yok."
                        else -> "Seçtiğin şehirlerde şu an açık ihale yok."
                    }
                    Note(R.drawable.ic_calendar_x, text, "Her şeyi göster", vm::showEverything)
                }
                else -> days(vm, groups, today, showCity = cities.size > 1)
            }
            item { Disclaimer(vm.index?.updatedAt?.let { updatedText(it, today) }, Modifier.padding(start = 20.dp, end = 20.dp, top = 32.dp)) }
        }
    }
}

@Composable
private fun Headline(title: String, sub: String) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = heading(40, 1.05f).copy(letterSpacing = (-0.025).em))
        Text(sub, style = type(17), color = palette.muted)
    }
}

// "Hepsi", then each chosen province, then "+" to choose more.
@Composable
private fun CityChips(vm: MainViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip("Hepsi", vm.cityFilter == null) { vm.cityFilter = null }
        if (vm.cities.size <= MOST_CHIPS) {
            vm.cities.forEach { slug ->
                Chip(vm.provinceName(slug) ?: slug, vm.cityFilter == slug) { vm.cityFilter = if (vm.cityFilter == slug) null else slug }
            }
        }
        Surface(onClick = vm::openSetup, shape = CircleShape, color = Color.Transparent, contentColor = palette.accentText, border = BorderStroke(1.dp, palette.line)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_plus), "Şehir ekle", Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) palette.text else Color.Transparent,
        contentColor = if (selected) palette.bg else palette.text,
        border = if (selected) null else BorderStroke(1.dp, palette.line),
    ) {
        Box(Modifier.heightIn(min = 42.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text(label, style = type(16)) }
    }
}

// Two weeks of days; the dots say how many tenders a day has (three at most), and touching a day shows only it.
@Composable
private fun DayStrip(today: LocalDate, picked: LocalDate?, tenders: List<Tender>, onPick: (LocalDate) -> Unit) {
    val counts = remember(tenders) { tenders.groupingBy { it.date }.eachCount() }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(STRIP_DAYS) { i ->
            val day = today.plusDays(i.toLong())
            val count = counts[day] ?: 0
            val on = day == picked
            Surface(
                onClick = { onPick(day) },
                shape = Sharp,
                color = if (on) palette.accent else Color.Transparent,
                contentColor = when { on -> palette.onAccent; count > 0 -> palette.text; else -> palette.faint },
            ) {
                Column(Modifier.widthIn(min = 50.dp).padding(top = 8.dp, bottom = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (i == 0) "Bugün" else day.weekdayShort(), style = type(13, lineHeight = 1.3f))
                    Text(day.dayOfMonth.toString(), style = type(22, FontWeight.SemiBold, 1.15f))
                    Row(Modifier.padding(top = 3.dp).height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(minOf(count, 3)) { Box(Modifier.size(5.dp).background(if (on) palette.onAccent else palette.accent, CircleShape)) }
                    }
                }
            }
        }
    }
}

// The sections of a list: a heading per day, then that day's tenders.
private fun LazyListScope.days(vm: MainViewModel, groups: List<Pair<LocalDate?, List<Tender>>>, today: LocalDate, showCity: Boolean) {
    groups.forEach { (day, tenders) ->
        item(key = "day-$day") {
            if (day == null) DayHeader("Tarihi belli değil", "", urgent = false)
            else DayHeader(dayTitle(day, today), dayMonth(day, today), urgent = day == today)
        }
        items(tenders, key = { it.ikn }) { tender ->
            val context = LocalContext.current
            TenderRow(tender, showCity, vm.isFavorite(tender), onOpen = { vm.selected = tender }) {
                Toast.makeText(context, if (vm.isFavorite(tender)) "Favorilerden çıkarıldı" else "Favorilere eklendi", Toast.LENGTH_SHORT).show()
                vm.toggleFavorite(tender)
            }
        }
    }
}

// Saved tenders, laid out like the agenda. They are kept whole on the device, so they stay after the bulletin
// drops them.
@Composable
private fun Saved(vm: MainViewModel) {
    val today = LocalDate.now()
    val groups = remember(vm.favorites) { vm.favorites.byDay() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Headline("${vm.favorites.size} favori ihale", "İlandan kalksalar da burada dururlar.") }
        if (groups.isEmpty()) item { Note(R.drawable.ic_star, "Henüz favori ihale yok. Ajandada bir ihalenin yanındaki yıldıza dokun.") }
        else days(vm, groups, today, showCity = true)
    }
}
