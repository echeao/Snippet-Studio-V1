package com.feige.snippetstudio.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.theme.CaptionStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.R_LG
import com.feige.snippetstudio.ui.theme.R_MD

/**
 * [LivePreviewBox] 设置页中的代码编辑器效果实时 Live 预览卡片。
 *
 * 架构职责：
 * 1. 实时呈现在设置中调节的代码字号 (sp)、软换行 (WordWrap) 与行号开闭效果。
 * 2. 帮助用户在修改偏好参数时无需退回编辑器即可直观评估排版视觉效果。
 *
 * @param fontSp 当前字号 (sp)
 * @param isWordWrap 是否开启软换行
 * @param showLineNumbers 是否显示行号
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun LivePreviewBox(
    fontSp: Float,
    isWordWrap: Boolean,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    val line1 = buildAnnotatedString {
        withStyle(SpanStyle(color = tc.primary, fontWeight = FontWeight.Bold)) {
            append("fun ")
        }
        withStyle(SpanStyle(color = tc.text)) {
            append("main() {")
        }
    }
    val line2 = buildAnnotatedString {
        append("    ")
        withStyle(SpanStyle(color = tc.primary)) {
            append("println")
        }
        withStyle(SpanStyle(color = tc.text)) {
            append("(")
        }
        withStyle(SpanStyle(color = Color(0xFF2E7D32))) {
            append("\"Hello Snippet Studio!\"")
        }
        withStyle(SpanStyle(color = tc.text)) {
            append(")")
        }
    }
    val line3 = buildAnnotatedString {
        withStyle(SpanStyle(color = tc.text)) {
            append("}")
        }
    }
    val sampleLines = listOf(line1, line2, line3)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tc.line.copy(alpha = if (tc.isDark) 0.15f else 0.08f), RoundedCornerShape(R_LG)),
        shape = RoundedCornerShape(R_LG),
        color = tc.surface2
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Window Header Bar with 3 dots and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFF27C93F), CircleShape))
                }

                Text(
                    text = "Live Sandbox · ${fontSp.toInt()}sp",
                    style = CaptionStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = tc.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = tc.codeBg,
                shape = RoundedCornerShape(R_MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tc.line.copy(alpha = if (tc.isDark) 0.12f else 0.05f), RoundedCornerShape(R_MD))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    sampleLines.forEachIndexed { index, line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showLineNumbers) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = fontSp.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = tc.text3.copy(alpha = 0.6f),
                                    modifier = Modifier.width(24.dp)
                                )
                            }
                            Text(
                                text = line,
                                fontSize = fontSp.sp,
                                lineHeight = (fontSp * 1.5f).sp,
                                fontFamily = FontFamily.Monospace,
                                color = tc.codeText,
                                maxLines = if (isWordWrap) Int.MAX_VALUE else 1
                            )
                        }
                    }
                }
            }
        }
    }
}

