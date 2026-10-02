@file:OptIn(ExperimentalLayoutApi::class)

package com.ogzhngms.ihalebak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.Category
import com.ogzhngms.ihalebak.MainViewModel
import com.ogzhngms.ihalebak.Period
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.filtered
import java.time.LocalDateTime

// Everything that narrows the list in one place: province, district, type and how soon. Each group is a row of
// large choices where exactly one is on, and the button at the bottom says how many tenders are left.
@Composable
fun FilterScreen(vm: MainViewModel, onBack: () -> Unit, onChangeProvince: () -> Unit) {
    val now = remember { LocalDateTime.now() }
    val open = remember(vm.tenders) { vm.tenders.filtered("", emptySet(), now) }
    val districts = remember(open) {
        open.mapNotNull { it.district }.groupingBy { it }.eachCount().toList().sortedWith(compareBy(TURKISH_ORDER) { it.first })
    }
    val left = remember(vm.tenders, vm.query, vm.category, vm.district, vm.period) {
        vm.tenders.filtered(vm.query, setOfNotNull(vm.category), now, vm.district, vm.period).size
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        BackRow("Filtrele", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Group("Şehir") {
                Surface(onClick = onChangeProvince, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_place), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(vm.provinceName(vm.province) ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("Değiştir", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (districts.size > 1) {
                Group("İlçe", note = "Kurumun bulunduğu ilçe") {
                    Choices(listOf<Pair<String?, String>>(null to "Tümü") + districts.map { (name, count) -> name to "$name ($count)" }, vm.district) { vm.district = it }
                }
            }
            Group("İhale türü") {
                Choices(listOf<Pair<Category?, String>>(null to "Tümü") + Category.entries.map { it to it.label }, vm.category) { vm.category = it }
            }
            Group("İhale tarihi") {
                Choices(Period.entries.map { it to it.label }, vm.period) { vm.period = it }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.clearFilters() }, enabled = vm.filterCount > 0, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text("Temizle", style = MaterialTheme.typography.titleSmall)
            }
            Button(onClick = onBack, modifier = Modifier.weight(2f).heightIn(min = 56.dp)) {
                Text(if (left == 0) "Uyan ihale yok" else "$left ihaleyi göster", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun Group(title: String, note: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

// One of these is always on; the chosen one is filled and ticked.
@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Surface(
                onClick = { onPick(value) },
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp),
                border = if (on) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (on) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(label, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
