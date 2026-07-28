package com.feige.snippetstudio.ui.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.AppSettingSwitchTile
import com.feige.snippetstudio.ui.editor.EditorUiState
import com.feige.snippetstudio.ui.editor.EditorViewModel
import com.feige.snippetstudio.ui.theme.CaptionStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors

/**
 * [EditorSettingsSheet] 编辑器个性化选项设置弹窗面板。
 *
 * 功能说明：
 * 1. 管理代码片段标签。
 * 2. 控制自动换行 (Word Wrap)、显示行号、高亮当前行、自动括号配对。
 * 3. 调节代码字号 (Slider 11sp ~ 22sp) 与选择字体族 (Monospace / SansSerif / Serif)。
 * 4. 切换字符编码格式 (UTF-8 / GBK 等) 与换行符类型 (LF / CRLF)。
 *
 * @param uiState 编辑器当前 UI 状态
 * @param viewModel 编辑器 ViewModel 实例
 * @param onClose 关闭面板回调
 * @param onOpenTagsDialog 打开标签编辑对话框回调
 * @param modifier 外部 Modifier
 */
@Composable
fun EditorSettingsSheet(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    onOpenTagsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部标题栏与关闭按键
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "编辑器选项",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tc.text
            )
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = tc.text2
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 属性分组 1: 片段标签管理 =====
        Text("片段属性", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTagsDialog() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "管理代码标签", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tc.text)
                Text(
                    text = if (uiState.tags.isEmpty()) "暂无标签，点击添加" else uiState.tags.joinToString(", ") { "#$it" },
                    style = CaptionStyle,
                    color = tc.text2
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = tc.text2
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 属性分组 2: 显示与排版开关 =====
        Text("显示与排版", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = "自动换行 (Word Wrap)",
            subTitle = "超出编辑器边缘时自动换行",
            checked = uiState.isWordWrap,
            onCheckedChange = { viewModel.setWordWrap(it) }
        )

        AppSettingSwitchTile(
            iconRes = R.drawable.ic_list,
            title = "显示行号 (Line Numbers)",
            subTitle = "在代码左侧展示行号栏",
            checked = uiState.showLineNumbers,
            onCheckedChange = { viewModel.setShowLineNumbers(it) }
        )

        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = "高亮当前行",
            subTitle = "高亮背景标记光标所在行",
            checked = uiState.highlightCurrentLine,
            onCheckedChange = { viewModel.setHighlightCurrentLine(it) }
        )

        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = "自动括号/引号配对",
            subTitle = "输入括号与引号时自动补全闭合",
            checked = uiState.autoPairBrackets,
            onCheckedChange = { viewModel.setAutoPairBrackets(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 字号调整 Slider =====
        Text("代码字号: ${uiState.fontSp} sp", style = CaptionStyle, color = tc.text, fontWeight = FontWeight.Medium)
        Slider(
            value = uiState.fontSp,
            onValueChange = { viewModel.adjustFontSize(it - uiState.fontSp) },
            valueRange = 11f..22f,
            steps = 11
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 字体族选择 =====
        Text("编辑器字体", style = CaptionStyle, color = tc.text, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        val fontOptions = listOf("monospace" to "等宽", "sans-serif" to "无衬线", "serif" to "衬线")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fontOptions.forEach { (code, label) ->
                FilterChip(
                    selected = (uiState.editorFontFamily == code),
                    onClick = { viewModel.setEditorFontFamily(code) },
                    label = { Text(label, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 属性分组 3: 编码与换行符 =====
        Text("编码与换行符", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text("字符编码格式", style = CaptionStyle, color = tc.text2)
        Spacer(modifier = Modifier.height(4.dp))
        val encodings = listOf("UTF-8", "GBK", "UTF-16", "ISO-8859-1", "US-ASCII")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            encodings.take(3).forEach { enc ->
                FilterChip(
                    selected = (uiState.encoding == enc),
                    onClick = { viewModel.setEncoding(enc) },
                    label = { Text(enc, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("换行符 (Line Ending)", style = CaptionStyle, color = tc.text2)
        Spacer(modifier = Modifier.height(4.dp))
        val lineEndings = listOf("LF (\n)" to "LF", "CRLF (\r\n)" to "CRLF")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lineEndings.forEach { (label, code) ->
                FilterChip(
                    selected = (uiState.lineEnding == code),
                    onClick = { viewModel.setLineEnding(code) },
                    label = { Text(label, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
