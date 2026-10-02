@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ogzhngms.ihalebak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.Period
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tab
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.filtered
import com.ogzhngms.ihalebak.searchKey
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun App(vm: MainViewModel) {
    val selected = vm.selected
    BackHandler(enabled = selected != null) { vm.selected = null }
    if (selected != null) {
        DetailScreen(vm, selected, onBack = { vm.selected = null })
        return
    }
    var picking by rememberSaveable { mutableStateOf(false) }
    var filtering by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = filtering) { filtering = false }
    if (filtering) {
        FilterScreen(vm, onBack = { filtering = false }, onChangeProvince = { filtering = false; picking = true })
        return
    }
    BackHandler(enabled = picking) { picking = false }
    BackHandler(enabled = !picking && vm.tab != Tab.TENDERS) { vm.tab = Tab.TENDERS }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İhaleBak", fontWeight = FontWeight.Bold) },
                // Navy in the light theme; in the dark theme a navy bar would be the pale "primary", so stay dark there.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(vm.tab == Tab.TENDERS, { vm.tab = Tab.TENDERS }, { Icon(painterResource(R.drawable.ic_list), null) }, label = { NavLabel("İhaleler") })
                NavigationBarItem(vm.tab == Tab.FAVORITES, { vm.tab = Tab.FAVORITES }, { Icon(painterResource(R.drawable.ic_star), null) }, label = { NavLabel("Kayıtlı") })
                NavigationBarItem(vm.tab == Tab.WATCH, { vm.tab = Tab.WATCH }, { Icon(painterResource(R.drawable.ic_bell), null) }, label = { NavLabel("Bildirimler") })
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (vm.tab) {
                Tab.TENDERS ->
                    if (vm.province == null || picking) {
                        ProvincePicker(
                            vm,
                            title = "Hangi şehrin ihalelerine bakalım?",
                            onPick = { vm.choose(it); picking = false },
                            onCancel = if (vm.province != null) ({ picking = false }) else null,
                        )
                    } else {
                        TendersScreen(vm, onChangeProvince = { picking = true }, onFilter = { filtering = true })
                    }
                Tab.FAVORITES -> FavoritesScreen(vm)
                Tab.WATCH -> WatchScreen(vm)
            }
        }
    }
}

@Composable
private fun NavLabel(text: String) = Text(text, maxLines = 1, softWrap = false)

// The 81 provinces in alphabetical order, as on any official form, each a large row with its number of open tenders.
@Composable
fun ProvincePicker(vm: MainViewModel, title: String, onPick: (String) -> Unit, onCancel: (() -> Unit)?) {
    var query by rememberSaveable { mutableStateOf("") }
    val index = vm.index
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Vazgeç") }
            }
            SearchField(query, { query = it }, "Şehir ara")
        }
        when {
            index == null && vm.loading -> Loading()
            index == null -> Problem("Şehir listesi yüklenemedi. İnternet bağlantını kontrol et.") { vm.load(refresh = true) }
            else -> {
                val needle = query.trim().searchKey()
                val provinces = remember(index, needle) {
                    index.provinces.filter { needle.isEmpty() || needle in it.name.searchKey() }.sortedWith(compareBy(TURKISH_ORDER) { it.name })
                }
                if (provinces.isEmpty()) Empty("\"$query\" adında bir şehir bulunamadı.")
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(provinces, key = { it.slug }) { province ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { onPick(province.slug) }.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(province.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                if (province.open == 0) "İhale yok" else "${province.open} ihale",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TendersScreen(vm: MainViewModel, onChangeProvince: () -> Unit, onFilter: () -> Unit) {
    val today = LocalDate.now()
    val all = remember(vm.tenders) { vm.tenders.filtered("", emptySet(), LocalDateTime.now()) }
    val shown = remember(vm.tenders, vm.query, vm.category, vm.district, vm.period) {
        vm.tenders.filtered(vm.query, setOfNotNull(vm.category), LocalDateTime.now(), vm.district, vm.period)
    }
    val narrowed = vm.query.isNotBlank() || vm.filterCount > 0
    PullToRefreshBox(isRefreshing = vm.loading && vm.tenders.isNotEmpty(), onRefresh = { vm.load(refresh = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ListHeader(vm, open = all.count { !it.cancelled }, onChangeProvince, onFilter) }
            when {
                vm.tenders.isEmpty() && vm.loading -> item { Loading() }
                vm.tenders.isEmpty() && vm.failed -> item { Problem("İhaleler yüklenemedi. İnternet bağlantını kontrol et.") { vm.load(refresh = true) } }
                shown.isEmpty() && !narrowed -> item { Empty("Bu şehirde şu an açık ihale görünmüyor.") }
                shown.isEmpty() -> item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Empty("Aradığına uyan ihale yok.")
                        OutlinedButton(onClick = { vm.query = ""; vm.clearFilters() }, modifier = Modifier.heightIn(min = 52.dp)) { Text("Aramayı ve filtreleri temizle") }
                    }
                }
                else -> {
                    if (narrowed) item { Text("${shown.size} ihale bulundu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    items(shown, key = { it.ikn }) { tender -> TenderCard(tender, today) { vm.selected = tender } }
                }
            }
            item { Disclaimer(Modifier.padding(top = 8.dp)) }
        }
    }
}

// Where (tap the province to change it) with "Filtrele" beside it, how many, search, and the filters that are on.
@Composable
private fun ListHeader(vm: MainViewModel, open: Int, onChangeProvince: () -> Unit, onFilter: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // "Filtrele" drops below the province when both do not fit, as with a long name at a large font size.
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = "Şehir değiştir", onClick = onChangeProvince).heightIn(min = 52.dp).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_place), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(vm.provinceName(vm.province) ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Icon(painterResource(R.drawable.ic_expand), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                FilledTonalButton(onClick = onFilter, modifier = Modifier.heightIn(min = 52.dp)) {
                    Icon(painterResource(R.drawable.ic_filter), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (vm.filterCount == 0) "Filtrele" else "Filtrele (${vm.filterCount})", style = MaterialTheme.typography.titleSmall)
                }
            }
            val counted = if (vm.tenders.isEmpty()) null else if (open == 0) "Açık ihale yok" else "$open açık ihale"
            val updated = vm.index?.updatedAt?.let { "Güncelleme: ${updatedText(it)}" }
            Text(listOfNotNull(counted, updated).joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SearchField(vm.query, { vm.query = it }, "İhale veya kurum ara")
        if (vm.filterCount > 0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.district?.let { ActiveFilter(it) { vm.district = null } }
                vm.category?.let { ActiveFilter(it.label) { vm.category = null } }
                if (vm.period.days != null) ActiveFilter(vm.period.label) { vm.period = Period.ALL }
            }
        }
        if (vm.offline && !vm.failed) {
            Text("İnternet yok, son indirilen bilgiler gösteriliyor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

// A filter that is on, with a cross to switch it off without opening the filter screen.
@Composable
private fun ActiveFilter(text: String, onRemove: () -> Unit) {
    Surface(onClick = onRemove, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(50)) {
        Row(Modifier.heightIn(min = 40.dp).padding(start = 14.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(painterResource(R.drawable.ic_close), "Kaldır", Modifier.size(18.dp))
        }
    }
}

// A tender card reads top to bottom in a fixed order: type and days left, what, who and where, then when.
@Composable
fun TenderCard(tender: Tender, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (tender.cancelled) 0.65f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(top = 14.dp)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryLabel(tender.category)
                    if (tender.cancelled) Tag("İptal edildi", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    else tender.remainingText()?.let { text ->
                        val over = text == "Geçti"
                        val soon = !over && (tender.daysLeft(today) ?: 99) <= 3
                        Tag(
                            text,
                            when { over -> MaterialTheme.colorScheme.surfaceVariant; soon -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.primaryContainer },
                            when { over -> MaterialTheme.colorScheme.onSurfaceVariant; soon -> MaterialTheme.colorScheme.onTertiaryContainer; else -> MaterialTheme.colorScheme.onPrimaryContainer },
                        )
                    }
                }
                Text(tender.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tender.authority?.let { InfoLine(R.drawable.ic_business, it, maxLines = 2) }
                    listOfNotNull(tender.district, tender.province).joinToString(", ").ifEmpty { null }?.let { InfoLine(R.drawable.ic_place, it, maxLines = 1) }
                }
            }
            Spacer(Modifier.size(12.dp))
            // The date sits in its own band along the bottom, so every card ends the same way.
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_event), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    tender.whenText(withYear = tender.date?.year != today.year) ?: "Tarih belirtilmemiş",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// The tender's type as a coloured dot and a word, so the four types can be told apart at a glance.
@Composable
private fun CategoryLabel(category: Category?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(categoryColor(category), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(category?.label ?: "İhale", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoLine(icon: Int, text: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
        trailingIcon = if (value.isNotEmpty()) ({ TextButton(onClick = { onChange("") }) { Text("Temizle") } }) else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun Empty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
    )
}

@Composable
fun Problem(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 52.dp)) { Text("Tekrar dene") }
    }
}

@Composable
fun BackRow(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) { Icon(painterResource(R.drawable.ic_back), "Geri") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
