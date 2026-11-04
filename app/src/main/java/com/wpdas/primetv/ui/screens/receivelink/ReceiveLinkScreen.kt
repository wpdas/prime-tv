package com.wpdas.primetv.ui.screens.receivelink

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.theme.InterFontFamily
import com.wpdas.primetv.ui.theme.PrimeTheme

@Composable
fun ReceiveLinkScreen(
    onBack: () -> Unit,
    onUseLink: (String) -> Unit,
    viewModel: ReceiveLinkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val actionFocus = remember { FocusRequester() }
    LaunchedEffect(state.receivedLink != null, state.expired, state.createError != null) { if (!state.creating) runCatching { actionFocus.requestFocus() } }

    Row(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = 96.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(Modifier.weight(1f).focusGroup(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BackLink(onClick = onBack)
            Text(t("screen.receive_link"), style = typography.screenTitle, color = colors.text)
            Text(t("receive.description"), style = typography.bodyLarge, color = colors.textSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
                Step(1, buildAnnotatedString {
                    append(t("receive.step1_a"))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.accentSoftText)) { append(siteLabel()) }
                    append(t("receive.step1_b"))
                })
                Step(2, buildAnnotatedString { append(t("receive.step2")) })
                Step(3, buildAnnotatedString { append(t("receive.step3")) })
            }
        }
        Column(
            Modifier.width(440.dp).background(colors.surface, PrimeTheme.shapes.panelLarge).padding(32.dp).focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val link = state.receivedLink
            val createError = state.createError
            if (state.creating) {
                Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) { LoadingSpinner(size = 36.dp) }
                Text(t("receive.creating"), style = typography.body, color = colors.textBody)
            } else if (createError != null) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.live, modifier = Modifier.size(44.dp))
                Text(
                    createError.serverMessage?.takeIf { it.isNotBlank() } ?: t(createError.messageKey, "detail" to createError.detail.orEmpty()),
                    style = typography.body,
                    color = colors.textBody,
                    textAlign = TextAlign.Center,
                )
                PrimeButton(text = t("common.retry"), onClick = viewModel::newCode, modifier = Modifier.focusRequester(actionFocus))
            } else if (link == null) {
                Text(t("receive.your_code"), style = typography.cardBody, color = colors.textSecondary)
                CodeBoxes(state.formattedCode)
                QrImage(state.qrUrl)
                if (state.expired) {
                    Text(t("receive.expired"), style = typography.body, color = colors.live)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LoadingSpinner(size = 16.dp, strokeWidth = 3.dp)
                        if (state.offline) Text(t("receive.offline"), style = typography.body, color = colors.live)
                        else Text(t("receive.waiting"), style = typography.body, color = colors.textBody)
                    }
                    Text(t("receive.expires_in", "time" to "%d:%02d".format(state.secondsLeft / 60, state.secondsLeft % 60)), style = typography.caption, color = colors.textSecondary)
                }
                PrimeButton(text = t("receive.new_code"), onClick = viewModel::newCode, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, modifier = Modifier.focusRequester(actionFocus))
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(44.dp))
                Text(t("receive.received"), style = typography.title, color = colors.text)
                Text(
                    state.maskedLink.orEmpty(),
                    style = typography.cardBody,
                    color = colors.textBody,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().background(colors.background, PrimeTheme.shapes.card).padding(horizontal = 14.dp, vertical = 12.dp),
                )
                state.detected?.let { TagChip(t("setup.detected", "type" to t(it.labelKey))) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimeButton(text = t("receive.use_link"), onClick = { onUseLink(link) }, modifier = Modifier.focusRequester(actionFocus))
                    PrimeButton(text = t("receive.another"), onClick = viewModel::newCode, style = PrimeButtonStyle.Secondary, horizontalPadding = 18.dp, textStyle = typography.buttonSmall)
                }
            }
        }
    }
}

private fun siteLabel(): String = BuildConfig.LINK_SITE_URL.removePrefix("https://").removePrefix("http://")

@Composable
private fun Step(number: Int, text: androidx.compose.ui.text.AnnotatedString) {
    val colors = PrimeTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(32.dp).background(colors.accentSoftBackground, CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = PrimeTheme.typography.bodyStrong, color = colors.accentSoftText)
        }
        Text(text, style = PrimeTheme.typography.tab.copy(fontWeight = FontWeight.Normal), color = colors.text)
    }
}

@Composable
private fun CodeBoxes(formatted: String) {
    val colors = PrimeTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        formatted.forEach { ch ->
            if (ch == '-') {
                Box(Modifier.size(16.dp, 52.dp), contentAlignment = Alignment.Center) { Text("-", color = colors.textSecondary) }
            } else {
                Box(Modifier.size(44.dp, 52.dp).background(colors.background, PrimeTheme.shapes.card), contentAlignment = Alignment.Center) {
                    Text(ch.toString(), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = colors.text)
                }
            }
        }
    }
}

/** QR gerado com ZXing para a URL do site já com o código. */
@Composable
private fun QrImage(content: String) {
    val colors = PrimeTheme.colors
    val bitmap = remember(content) { if (content.isBlank()) null else qrBitmap(content, 296) }
    Box(Modifier.size(148.dp).background(colors.text, PrimeTheme.shapes.card).padding(10.dp), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = content, modifier = Modifier.fillMaxSize()) }
    }
}

private fun qrBitmap(content: String, size: Int): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) 0xFF0B0B0F.toInt() else 0xFFF2F2F2.toInt() }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}.getOrNull()
