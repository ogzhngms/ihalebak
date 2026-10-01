@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ogzhngms.ihalebak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tab
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.filtered
import com.ogzhngms.ihalebak.lowercaseTr
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
    BackHandler(enabled = picking) { picking = false }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("İhaleBak", fontWeight = FontWeight.Bold)
                        val subtitle = when (vm.tab) {
                            Tab.TENDERS -> vm.provinceName(vm.province)?.let { "$it ihaleleri" }
                            Tab.FAVORITES -> "Kaydettiğin ihaleler"
                            Tab.WATCH -> "Yeni ihale bildirimleri"
                        }
                        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(vm.tab == Tab.TENDERS, { vm.tab = Tab.TENDERS }, { Icon(painterResource(R.drawable.ic_list), null) }, label = { Text("İhaleler") })
                NavigationBarItem(vm.tab == Tab.FAVORITES, { vm.tab = Tab.FAVORITES }, { Icon(painterResource(R.drawable.ic_star), null) }, label = { Text("Favoriler") })
                NavigationBarItem(vm.tab == Tab.WATCH, { vm.tab = Tab.WATCH }, { Icon(painterResource(R.drawable.ic_bell), null) }, label = { Text("Takip") })
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (vm.tab) {
                Tab.TENDERS ->
                    if (vm.province == null || picking) {
                        ProvincePicker(vm, onPick = { vm.choose(it); picking = false }, onCancel = if (vm.province != null) ({ picking = false }) else null)
                    } else {
                        TendersScreen(vm, onChangeProvince = { picking = true })
                    }
                Tab.FAVORITES -> FavoritesScreen(vm)
                Tab.WATCH -> WatchScreen(vm)
            }
        }
    }
}

// The 81 provinces with how many open tenders each has, searchable; the busiest come first.
@Composable
private fun ProvincePicker(vm: MainViewModel, onPick: (String) -> Unit, onCancel: (() -> Unit)?) {
    var query by rememberSaveable { mutableStateOf("") }
    val index = vm.index
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hangi ilin ihalelerine bakalım?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Vazgeç") }
            }
            SearchField(query, { query = it }, "İl ara")
        }
        when {
            index == null && vm.loading -> Loading()
            index == null -> Problem("İl listesi yüklenemedi. İnternet bağlantını kontrol et.") { vm.load(refresh = true) }
            else -> {
                val needle = query.trim().lowercaseTr()
                val provinces = index.provinces.filter { needle.isEmpty() || needle in it.name.lowercaseTr() }.sortedByDescending { it.open }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(provinces, key = { it.slug }) { province ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(province.slug) }.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_place), null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(province.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            CountBadge(province.open)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TendersScreen(vm: MainViewModel, onChangeProvince: () -> Unit) {
    val today = LocalDate.now()
    val shown = remember(vm.tenders, vm.query, vm.categories) { vm.tenders.filtered(vm.query, vm.categories, LocalDateTime.now()) }
    PullToRefreshBox(isRefreshing = vm.loading && vm.tenders.isNotEmpty(), onRefresh = { vm.load(refresh = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onChangeProvince) {
                            Icon(painterResource(R.drawable.ic_place), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(vm.provinceName(vm.province) ?: "İl seç")
                            Icon(painterResource(R.drawable.ic_expand), null, Modifier.size(18.dp))
                        }
                    }
                    SearchField(vm.query, { vm.query = it }, "İhale, kurum ya da İKN ara")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.entries.forEach { category ->
                            FilterChip(selected = category in vm.categories, onClick = { vm.toggleCategory(category) }, label = { Text(category.label) })
                        }
                    }
                    if (vm.tenders.isNotEmpty()) StatusLine(vm, open = shown.count { !it.cancelled }, cancelled = shown.count { it.cancelled })
                }
            }
            when {
                vm.tenders.isEmpty() && vm.loading -> item { Loading() }
                vm.tenders.isEmpty() && vm.failed -> item { Problem("İhaleler yüklenemedi. İnternet bağlantını kontrol et.") { vm.load(refresh = true) } }
                shown.isEmpty() -> item {
                    Empty(if (vm.tenders.isEmpty()) "Bu ilde şu an açık ihale görünmüyor." else "Aramana uyan ihale yok.")
                }
                else -> items(shown, key = { it.ikn }) { tender -> TenderCard(tender, today, vm.isFavorite(tender)) { vm.selected = tender } }
            }
        }
    }
}

@Composable
private fun StatusLine(vm: MainViewModel, open: Int, cancelled: Int) {
    val updated = vm.index?.updatedAt?.let { "güncellendi ${updatedText(it)}" }
    val text = listOfNotNull(
        "$open açık ihale",
        if (cancelled > 0) "$cancelled iptal" else null,
        updated,
        if (vm.offline) "çevrimdışı" else null,
    ).joinToString(" · ")
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TenderCard(tender: Tender, today: LocalDate, favorite: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (tender.cancelled) 0.6f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tender.category?.label?.uppercase(java.util.Locale.forLanguageTag("tr")) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (favorite) Icon(painterResource(R.drawable.ic_star), "Favori", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                if (tender.cancelled) Tag("İptal edildi", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                else daysLeftText(tender.daysLeft(today))?.let { days ->
                    val soon = (tender.daysLeft(today) ?: 99) <= 3
                    Tag(
                        days,
                        if (soon) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        if (soon) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(tender.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            tender.authority?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            tender.whenText()?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_event), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun Tag(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun CountBadge(count: Int) {
    val active = count > 0
    Tag(
        "$count",
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
        singleLine = true,
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
    Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp))
}

@Composable
fun Problem(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRetry) { Text("Tekrar dene") }
    }
}

@Composable
fun BackRow(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), "Geri") }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
}
