package com.feige.snippetstudio.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SyntaxHighlighter
import com.feige.snippetstudio.util.SyntaxLanguageDetector

/**
 * [TempFilePreviewScreen] 分享文件临时预览界面（只读模式，不保存至本地/数据库）。
 *
 * 核心逻辑：
 * 1. 使用 [remember(content)] 记住 [savedContent] 引用，防止屏幕旋转或系统配置更改导致预览内容丢失，
 *    同时避免使用 rememberSaveable 触发 Bundle 序列化导致超大文件引发 Binder TransactionTooLargeException 崩溃。
 * 2. 通过 [SyntaxLanguageDetector] 推断语法语言，并利用 [SyntaxHighlighter] 进行富文本语法高亮展示。
 * 3. 使用 [SelectionContainer] 支持文本选中与自由复制。
 *
 * @param content 待预览的原始文本内容
 * @param fileName 来源文件名（可选）
 * @param typeCode 片段语言类型编码（如 "html", "js", "markdown" 等）
 * @param onBack 关闭预览返回上一页回调
 * @param onShowSnackbar 显示提示消息回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempFilePreviewScreen(
    content: String,
    fileName: String?,
    typeCode: String,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val tc = LocalThemeColors.current
    // 使用 remember(content) 保持大文本引用，规避 Binder 溢出限制
    val savedContent = remember(content) { content }
    val snippetType = SnippetType.fromCode(typeCode)
    val displayName = fileName ?: snippetType.displayName

    val language = SyntaxLanguageDetector.detect(fileName ?: "", snippetType)
    val isDark = tc.isDark

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = tc.text
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = tc.text
                        )
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        Surface(
                            color = tc.primarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = snippetType.displayName,
                                style = com.feige.snippetstudio.ui.theme.BadgeStyle,
                                color = tc.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg)
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(tc.surface)
                .padding(Spacing.S4)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = SyntaxHighlighter.highlightByLanguage(savedContent, language, isDark),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = tc.text
            )
        }
    }
}
