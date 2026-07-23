package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

@Composable
fun SymbolBar(
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val symbols = listOf("<", ">", "/", "=", "\"", "'", "!", "<!-- -->", "{", "}", "(", ")", "[", "]", ";", ":")
    val borderColor = if (isDark) LineDark else LineLight
    val btnBg = if (isDark) SurfaceDark else SurfaceLight
    val textPrimary = if (isDark) TextDark else TextLight
    val labelColor = if (isDark) Text3Dark else Text3Light

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (isDark) Surface2Dark else Surface2Light)
            .padding(horizontal = Spacing.S3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.editor_symbols),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            color = labelColor,
            modifier = Modifier.padding(end = Spacing.S2)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            items(symbols) { symbol ->
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .padding(vertical = 2.dp)
                        .background(btnBg, RoundedCornerShape(R_SM))
                        .border(1.dp, borderColor, RoundedCornerShape(R_SM))
                        .clickable { onInsertSymbol(symbol) }
                        .padding(horizontal = Spacing.S3)
                        .testTag("symbol_btn_$symbol"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.W600,
                        color = textPrimary
                    )
                }
            }
        }
    }
}
