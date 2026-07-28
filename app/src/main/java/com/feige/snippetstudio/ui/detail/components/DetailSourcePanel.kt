package com.feige.snippetstudio.ui.detail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.theme.CaptionStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.R_SM
import com.feige.snippetstudio.ui.theme.Spacing
import com.feige.snippetstudio.util.SyntaxHighlighter
import com.feige.snippetstudio.util.SyntaxLanguageDetector

/**
 * [DetailSourcePanel] 代码片段源码高亮查看与折叠阅读面板。
 *
 * 核心功能：
 * 1. 结合 [SyntaxLanguageDetector] 自动推断编程语言类型。
 * 2. 结合 [SyntaxHighlighter] 生成具备浅色/深色主题兼容的 [AnnotatedString] 高亮富文本。
 * 3. 提供具备优雅阴影与边框的代码块，自动对齐行号边栏（Line Numbers）。
 * 4. 内置 `animateContentSize` 过渡平滑动画，响应展开与收起动作。
 * 5. 顶部操作工具栏内置一键复制代码与收起/展开切换控制。
 *
 * @param snippet 当前代码片段实体
 * @param isExpanded 当前源代码卡片是否处于展开显示全量状态
 * @param onToggleExpanded 切换展开/折叠状态回调
 * @param onShowSnackbar 底部 Toast/Snackbar 提示闭包
 * @param modifier 外部布局修饰符
 */
@Composable
fun DetailSourcePanel(
    snippet: Snippet,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    // 1. 自动检测推断语法高亮语言类型
    val language = remember(snippet.fileName, snippet.type) {
        SyntaxLanguageDetector.detect(snippet.fileName, snippet.type)
    }

    // 2. 根据展开/折叠状态裁切显示的文本行数
    val displayedContent = remember(snippet.content, isExpanded) {
        if (isExpanded) {
            snippet.content
        } else {
            snippet.content.lines().take(8).joinToString("\n")
        }
    }

    // 3. 动态生成带行号对齐的编号列文本
    val lineNumbersText = remember(displayedContent) {
        val lineCount = displayedContent.lines().size
        (1..lineCount).joinToString("\n")
    }

    // 4. 使用 SyntaxHighlighter 缓存生成高亮 AnnotatedString
    val highlightedText = remember(displayedContent, language, isDark) {
        SyntaxHighlighter.highlightByLanguage(displayedContent, language, isDark)
    }

    DetailPanel(
        title = stringResource(R.string.detail_source),
        modifier = modifier,
        headerAction = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 复制代码按钮
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(snippet.content))
                        onShowSnackbar(context.getString(R.string.toast_copied))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "复制代码",
                        tint = tc.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.S2))

                // 展开/收起切换按钮
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    style = CaptionStyle,
                    color = tc.primary,
                    modifier = Modifier.clickable(onClick = onToggleExpanded)
                )
            }
        }
    ) {
        // 带有高度平滑变动 animateContentSize 动效的代码容器卡片
        Surface(
            color = tc.surface2,
            shape = RoundedCornerShape(R_SM),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.S3)
            ) {
                // ===== 左侧：行号指示栏 (Line Numbers) =====
                Text(
                    text = lineNumbersText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = tc.text2.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = Spacing.S3)
                )

                // 行号分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(tc.line)
                )

                Spacer(modifier = Modifier.width(Spacing.S3))

                // ===== 右侧：语法高亮正文内容 =====
                Text(
                    text = highlightedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = tc.text,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
