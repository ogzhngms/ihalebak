package com.ogzhngms.ihalebak.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ogzhngms.ihalebak.R
import com.ogzhngms.ihalebak.Tender
import com.ogzhngms.ihalebak.isOver
import java.time.LocalDateTime

const val EKAP_HOME = "https://ekap.kik.gov.tr"

// The design's corners are almost square.
val Sharp = RoundedCornerShape(2.dp)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Int? = null, minHeight: Dp = 56.dp, size: Int = 17) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        enabled = enabled,
        shape = Sharp,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            disabledContainerColor = palette.accent.copy(alpha = 0.4f),
            disabledContentColor = palette.onAccent,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = type(size, FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, minHeight: Dp = 56.dp, size: Int = 17) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        shape = Sharp,
        border = BorderStroke(1.dp, palette.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) { Text(text, style = type(size, FontWeight.SemiBold), maxLines = 1) }
}

// A square outlined button holding only an icon; the label is read out by screen readers.
@Composable
fun SquareButton(icon: Int, label: String, onClick: () -> Unit, tint: Color = palette.text, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        enabled = enabled,
        shape = Sharp,
        border = BorderStroke(1.dp, palette.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint, disabledContentColor = palette.faint),
        contentPadding = PaddingValues(0.dp),
    ) { Icon(painterResource(icon), label, Modifier.size(24.dp)) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = Sharp,
        colors = ButtonDefaults.textButtonColors(contentColor = palette.accentText),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = type(16, FontWeight.SemiBold))
    }
}

@Composable
fun IconTap(icon: Int, label: String, onClick: () -> Unit, tint: Color = palette.text) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(painterResource(icon), label, Modifier.size(24.dp), tint = tint) }
}

@Composable
fun SearchInput(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, focus: Boolean = false) {
    val requester = remember { FocusRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp).focusRequester(requester),
        placeholder = { Text(hint, style = type(17), color = palette.muted) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(22.dp), tint = palette.muted) },
        trailingIcon = if (value.isNotEmpty()) ({ IconTap(R.drawable.ic_close, "Temizle", { onChange("") }, palette.muted) }) else null,
        singleLine = true,
        textStyle = type(17),
        shape = Sharp,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.line,
            cursorColor = palette.accent,
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
        ),
    )
    if (focus) LaunchedEffect(Unit) { requester.requestFocus() }
}

// A day's heading: "Bugün  2 Ekim". Today's is in the urgent colour.
@Composable
fun DayHeader(title: String, sub: String, urgent: Boolean) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = type(23, FontWeight.SemiBold, 1.2f), color = if (urgent) palette.urgent else palette.text, modifier = Modifier.alignByBaseline())
        Text(sub, style = type(16), color = palette.muted, modifier = Modifier.alignByBaseline())
    }
}

// One tender in a list: the hour (and the province, when several are shown) on the left, then what, who and
// which type, and the favourite star on the right.
@Composable
fun TenderRow(tender: Tender, showCity: Boolean, favorite: Boolean, onOpen: () -> Unit, onStar: () -> Unit) {
    val over = !tender.cancelled && tender.isOver(LocalDateTime.now())
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp).alpha(if (tender.cancelled) 0.6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.widthIn(min = 62.dp, max = 100.dp).padding(top = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tender.time?.toString() ?: "—", style = type(20, FontWeight.SemiBold, 1.25f).copy(fontFeatureSettings = "tnum"), color = palette.accentText)
            if (showCity) Text(tender.province.orEmpty(), style = type(14), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tender.title, style = type(19, FontWeight.SemiBold, 1.25f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            tender.authority?.let { Text(it, style = type(15, lineHeight = 1.35f), color = palette.muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Text(
                when {
                    tender.cancelled -> "İptal edildi"
                    over -> "Tarihi geçti"
                    else -> tender.category?.label ?: "İhale"
                },
                style = type(14),
                color = if (tender.cancelled) palette.urgent else palette.muted,
            )
        }
        IconTap(
            if (favorite) R.drawable.ic_star_on else R.drawable.ic_star,
            if (favorite) "Favorilerden çıkar" else "Favorilere ekle",
            onStar,
            if (favorite) palette.star else palette.faint,
        )
    }
}

// An empty or failed state: a large pale icon, a sentence, and one thing to do about it.
@Composable
fun Note(icon: Int, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(icon), null, Modifier.size(52.dp), tint = palette.faint)
        Text(text, style = type(18, lineHeight = 1.5f), color = palette.muted)
        if (action != null) SecondaryButton(action, onAction, minHeight = 52.dp, size = 16)
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = palette.accent) }
}

// Google Play asks apps that show government information to say plainly that they are not official and to
// name their source; this closes every list.
@Composable
fun Disclaimer(updated: String?, modifier: Modifier = Modifier) {
    val link = TextLinkStyles(SpanStyle(color = palette.accentText, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        append("İhaleBak resmi bir devlet uygulaması değildir; Kamu İhale Kurumu veya EKAP ile bağlantısı yoktur. ")
        append("Bilgiler Kamu İhale Bülteni'nden otomatik derlenir ve hata içerebilir. Kesin bilgi: ")
        withLink(LinkAnnotation.Url(EKAP_HOME, link)) { append("ekap.kik.gov.tr") }
        if (updated != null) append(" · Güncelleme $updated")
    }
    Text(text, modifier, style = type(14, lineHeight = 1.5f), color = palette.muted)
}
