package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.InterFontFamily
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Quadrado violeta com "P" branco. [size] 36 na barra superior, 56 no Setup. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val colors = PrimeTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.accent, RoundedCornerShape(size * 8 / 36)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "P",
            color = colors.onAccent,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 20 / 36).sp,
        )
    }
}

/** Wordmark "PrimeTV" com o "TV" em violeta. */
@Composable
fun LogoWordmark(modifier: Modifier = Modifier, fontSize: Int = 20) {
    val colors = PrimeTheme.colors
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append("Prime")
            withStyle(SpanStyle(color = colors.accent)) { append("TV") }
        },
        style = PrimeTheme.typography.logo.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.2f).sp),
        color = colors.text,
    )
}

@Composable
fun Logo(modifier: Modifier = Modifier, markSize: Dp = 36.dp, fontSize: Int = 20, gap: Dp = 10.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
        LogoMark(size = markSize)
        LogoWordmark(fontSize = fontSize)
    }
}
