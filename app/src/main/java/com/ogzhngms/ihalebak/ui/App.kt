@file:OptIn(ExperimentalMaterial3Api::class)

package com.ogzhngms.ihalebak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
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
                title = { Text("İhaleBak", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(vm.tab == Tab.TENDERS, { vm.tab = Tab.TENDERS }, { Icon(painterResource(R.drawable.ic_list), null) }, label = { Text("İhaleler") })
                NavigationBarItem(vm.tab == Tab.FAVORITES, { vm.tab = Tab.FAVORITES }, { Icon(painterResource(R.drawable.ic_star), null) }, label = { Text("Kaydettiklerim") })
                NavigationBarItem(vm.tab == Tab.WATCH, { vm.tab = Tab.WATCH }, { Icon(painterResource(R.drawable.ic_bell), null) }, label = { Text("Bildirimler") })
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
                        TendersScreen(vm, onChangeProvince = { picking = true })
                    }
                Tab.FAVORITES -> FavoritesScreen(vm)
                Tab.WATCH -> WatchScreen(vm)
            }
        }
    }
}

// The 81 provinces, busiest first, each a large row with its number of open tenders.
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
                val needle = query.trim().lowercaseTr()
                val provinces = index.provinces.filter { needle.isEmpty() || needle in it.name.lowercaseTr() }.sortedByDescending { it.open }
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
private fun TendersScreen(vm: MainViewModel, onChangeProvince: () -> Unit) {
    val today = LocalDate.now()
    val shown = remember(vm.tenders, vm.query, vm.category) { vm.tenders.filtered(vm.query, setOfNotNull(vm.category), LocalDateTime.now()) }
    PullToRefreshBox(isRefreshing = vm.loading && vm.tenders.isNotEmpty(), onRefresh = { vm.load(refresh = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // The province, large, with a plain "Değiştir" so it is obvious how to switch.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(vm.provinceName(vm.province) ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            if (vm.tenders.isNotEmpty()) {
                                val open = shown.count { !it.cancelled }
                                Text(
                                    if (open == 0) "Açık ihale yok" else "$open açık ihale",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(onClick = onChangeProvince, modifier = Modifier.heightIn(min = 48.dp)) { Text("Değiştir") }
                    }
                    SearchField(vm.query, { vm.query = it }, "İhale veya kurum ara")
                    CategoryPicker(vm.category) { vm.category = it }
                    vm.index?.updatedAt?.let {
                        Text("Son güncelleme: ${updatedText(it)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (vm.offline) {
                        Text("İnternet yok, son indirilen bilgiler gösteriliyor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            when {
                vm.tenders.isEmpty() && vm.loading -> item { Loading() }
                vm.tenders.isEmpty() && vm.failed -> item { Problem("İhaleler yüklenemedi. İnternet bağlantını kontrol et.") { vm.load(refresh = true) } }
                shown.isEmpty() -> item {
                    Empty(if (vm.query.isBlank() && vm.category == null) "Bu şehirde şu an açık ihale görünmüyor." else "Aramana uyan ihale yok.")
                }
                else -> items(shown, key = { it.ikn }) { tender -> TenderCard(tender, today) { vm.selected = tender } }
            }
            item { Disclaimer(Modifier.padding(top = 8.dp)) }
        }
    }
}

// One button that says what is shown ("Tür: Tümü") and opens a short list of large choices.
@Composable
private fun CategoryPicker(selected: Category?, onPick: (Category?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Tür: " + (selected?.label ?: "Tümü"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_expand), null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf<Category?>(null) + Category.entries).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option?.label ?: "Tümü",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onPick(option); open = false },
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            }
        }
    }
}

// A tender in three lines: what, who, and when, with the days left in plain words.
@Composable
fun TenderCard(tender: Tender, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (tender.cancelled) 0.65f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tender.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            tender.authority?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tender.whenText() ?: "Tarih belirtilmemiş",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
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
        }
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
