package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.model.DiffLine
import com.feige.snippetstudio.model.DiffType
import com.feige.snippetstudio.ui.theme.*

@Composable
fun DiffViewer(
    diffLines: List<DiffLine>,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    val hScrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tc.surface2.copy(alpha = 0.3f), RoundedCornerShape(R_SM))
            .padding(Spacing.S1)
            .horizontalScroll(hScrollState)
            .verticalScroll(rememberScrollState())
    ) {
        diffLines.forEach { line ->
            // 根据明暗主题与 Diff 行类型 (ADD / DELETE / CONTEXT) 动态算色彩
            val bgColor = when (line.type) {
                DiffType.ADD -> SyncGreen.copy(alpha = if (tc.isDark) 0.25f else 0.12f)
                DiffType.DELETE -> SyncRed.copy(alpha = if (tc.isDark) 0.25f else 0.12f)
                DiffType.CONTEXT -> androidx.compose.ui.graphics.Color.Transparent
            }
            val fgColor = when (line.type) {
                DiffType.ADD -> SyncGreen
                DiffType.DELETE -> SyncRed
                DiffType.CONTEXT -> tc.text2
            }
            val prefix = when (line.type) {
                DiffType.ADD -> "+"
                DiffType.DELETE -> "-"
                DiffType.CONTEXT -> " "
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = Spacing.S1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 行号区域
                val lineNum = buildString {
                    if (line.oldLineNum != null) append(line.oldLineNum)
                    append(" ")
                    if (line.newLineNum != null) append(line.newLineNum)
                }
                Text(
                    text = lineNum.padStart(8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = tc.text3,
                    modifier = Modifier.width(64.dp)
                )

                Text(
                    text = "$prefix ${line.content}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = fgColor,
                    fontWeight = if (line.type != DiffType.CONTEXT) FontWeight.W500 else FontWeight.Normal
                )
            }
        }
    }
}
