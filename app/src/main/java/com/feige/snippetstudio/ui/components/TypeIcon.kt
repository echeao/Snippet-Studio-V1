package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val isDark = isSystemInDarkTheme()

    val (bg, fg) = when (type) {
        SnippetType.HTML -> if (isDark) C_Html.copy(alpha = 0.2f) to C_Html else C_HtmlBg to C_Html
        SnippetType.JS -> if (isDark) C_Js.copy(alpha = 0.2f) to C_Js else C_JsBg to C_Js
        SnippetType.MARKDOWN -> if (isDark) C_Md.copy(alpha = 0.2f) to C_Md else C_MdBg to C_Md
        SnippetType.PROMPT -> if (isDark) C_Prompt.copy(alpha = 0.2f) to Primary2 else C_PromptBg to C_Prompt
    }

    Box(
        modifier = modifier
            .size(size)
            .background(bg, RoundedCornerShape(R_SM)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            SnippetType.HTML -> {
                Text(
                    text = "HTML",
                    color = fg,
                    fontSize = (size.value * 0.28f).sp,
                    fontWeight = FontWeight.W900,
                    fontFamily = FontFamily.Monospace
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
                    painter = painterResource(id = R.drawable.ic_markdown),
                    contentDescription = "Markdown",
                    tint = fg,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            SnippetType.PROMPT -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_spark),
                    contentDescription = "Prompt",
                    tint = fg,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
