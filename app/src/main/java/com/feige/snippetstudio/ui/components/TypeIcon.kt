package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

@Composable
fun TypeIcon(
    type: SnippetType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val tc = LocalThemeColors.current
    val style = LocalColorThemeStyle.current
    val palette = ColorThemeRegistry.paletteOf(style)
    val iconColors = palette.typeIcons

    val fg = when (type) {
        SnippetType.HTML -> iconColors.html
        SnippetType.JS -> iconColors.js
        SnippetType.MARKDOWN -> iconColors.md
        SnippetType.PROMPT -> iconColors.prompt
        SnippetType.JAVA -> iconColors.html // 使用暖色调突出 Java
        SnippetType.GENERAL -> iconColors.prompt
    }
    val bg = if (tc.isDark) fg.copy(alpha = 0.2f) else {
        when (type) {
            SnippetType.HTML -> C_HtmlBg
            SnippetType.JS -> C_JsBg
            SnippetType.MARKDOWN -> C_MdBg
            SnippetType.PROMPT -> C_PromptBg
            SnippetType.JAVA -> C_HtmlBg
            SnippetType.GENERAL -> C_PromptBg
        }
    }

    val mdRes = when (style.iconSuffix) {
        "ocean" -> R.drawable.ic_md_ocean
        "sunset" -> R.drawable.ic_md_sunset
        "lavender" -> R.drawable.ic_md_lavender
        "mono" -> R.drawable.ic_md_mono
        else -> R.drawable.ic_md_forest
    }
    val sparkRes = when (style.iconSuffix) {
        "ocean" -> R.drawable.ic_spark_ocean
        "sunset" -> R.drawable.ic_spark_sunset
        "lavender" -> R.drawable.ic_spark_lavender
        "mono" -> R.drawable.ic_spark_mono
        else -> R.drawable.ic_spark_forest
    }

    Box(
        modifier = modifier
            .size(size)
            .background(bg, RoundedCornerShape(R_SM)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            SnippetType.HTML -> {
                // 渲染 HTML 文件的矢量 SVG 图标
                Icon(
                    painter = painterResource(id = R.drawable.ic_html),
                    contentDescription = "HTML",
                    tint = fg,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            SnippetType.JS -> {
                Text(
                    text = "</>",
                    color = fg,
                    fontSize = (size.value * 0.35f).sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = FontFamily.Monospace
                )
            }
            SnippetType.MARKDOWN -> {
                Icon(
                    painter = painterResource(id = mdRes),
                    contentDescription = "Markdown",
                    tint = fg,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            SnippetType.PROMPT -> {
                Icon(
                    painter = painterResource(id = sparkRes),
                    contentDescription = "Prompt",
                    tint = fg,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            SnippetType.JAVA -> {
                Text(
                    text = "J",
                    color = fg,
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = FontFamily.Monospace
                )
            }
            SnippetType.GENERAL -> {
                Text(
                    text = "T",
                    color = fg,
                    fontSize = (size.value * 0.35f).sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
