package com.feige.snippetstudio.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.theme.CaptionStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface2
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "实时效果预览 (Live Preview)",
                style = CaptionStyle,
                color = tc.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = tc.bg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    val codeLines = listOf(
                        "fun main() {",
                        "    println(\"Hello Snippet Studio!\")",
                        "}"
                    )
                    codeLines.forEachIndexed { index, line ->
                        Row {
                            if (showLineNumbers) {
                                Text(
                                    text = "${index + 1} ",
                                    fontSize = fontSp.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = tc.text2.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = line,
                                fontSize = fontSp.sp,
                                fontFamily = FontFamily.Monospace,
                                color = tc.text,
                                maxLines = if (isWordWrap) Int.MAX_VALUE else 1
                            )
                        }
                    }
                }
            }
        }
    }
}
