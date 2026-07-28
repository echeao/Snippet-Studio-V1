package com.feige.snippetstudio.ui.editor.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.editor.SaveState
import com.feige.snippetstudio.ui.theme.*

/**
 * [EditorTopAppBar] 代码编辑器顶层 App 导航条组件。
 *
 * 架构职责：
 * 1. 提供返回导航按键与未保存更改二次确认。
 * 2. 承载代码片段标题直接修改 [BasicTextField]，并实时展示保存状态指示徽章（已保存 / 正在保存 / 未保存）。
 * 3. 承载编辑器右上角 Dropdown 菜单（包含编辑选项设置、标签修改、Prompt 变量填充、复制代码与强制立即保存等）。
 *
 * @param title 当前代码片段标题
 * @param saveState 当前保存状态 [SaveState]
 * @param snippetType 当前代码片段类型 [SnippetType]
 * @param hasPromptVariables 是否包含 Prompt 变量
 * @param snippetId 片段唯一 ID（用于判断是否展示 Git 历史）
 * @param onTitleChange 标题更动回调
 * @param onBack 点击返回键回调
 * @param onOpenSettingsSheet 打开编辑器设置 BottomSheet 回调
 * @param onOpenTagDialog 打开标签编辑对话框回调
 * @param onToggleVariablePanel 打开 Prompt 变量填充面板回调
 * @param onForceSave 点击强制立即保存回调
 * @param onShowSnackbar 显示提示消息回调
 * @param onNavigateToHistory 跳转 Git 版本历史回调
 * @param modifier 外部 Modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopAppBar(
    title: String,
    saveState: SaveState,
    snippetType: SnippetType,
    hasPromptVariables: Boolean,
    snippetId: String,
    codeContent: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onOpenTagDialog: () -> Unit,
    onToggleVariablePanel: () -> Unit,
    onForceSave: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onNavigateToHistory: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tc = LocalThemeColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    val saveBadgeText = when (saveState) {
        SaveState.SAVED -> stringResource(R.string.state_saved)
        SaveState.SAVING -> stringResource(R.string.state_saving)
        SaveState.UNSAVED -> stringResource(R.string.state_unsaved)
    }

    val saveBadgeBg = when (saveState) {
        SaveState.SAVED -> SuccessSoft
        SaveState.SAVING -> WarningSoft
        SaveState.UNSAVED -> DangerSoft
    }

    val saveBadgeFg = when (saveState) {
        SaveState.SAVED -> Success
        SaveState.SAVING -> Warning
        SaveState.UNSAVED -> Danger
    }

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_btn")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = tc.text
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 片段标题在线修改框
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W800,
                        color = tc.text
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(tc.primary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_title_input"),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_rename_hint),
                                style = TextStyle(fontSize = 16.sp, color = tc.text2)
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(Spacing.S2))

                // 保存状态指示徽章 (Saved / Saving / Unsaved)
                Surface(
                    color = saveBadgeBg,
                    shape = RoundedCornerShape(R_SM)
                ) {
                    Text(
                        text = saveBadgeText,
                        style = BadgeStyle,
                        color = saveBadgeFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        actions = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("editor_more_menu_btn")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "More",
                        tint = tc.text
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // 编辑区设置
                    DropdownMenuItem(
                        text = { Text("编辑区设置", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenSettingsSheet()
                        },
                        modifier = Modifier.testTag("menu_editor_settings")
                    )

                    // 编辑片段标签
                    DropdownMenuItem(
                        text = { Text("编辑片段标签", style = CaptionStyle) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tag),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenTagDialog()
                        }
                    )

                    // Prompt 变量填充快捷入口
                    if (snippetType == SnippetType.PROMPT && hasPromptVariables) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.var_menu_fill), style = CaptionStyle) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_spark),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleVariablePanel()
                            }
                        )
                    }

                    // 复制全量代码
                    DropdownMenuItem(
                        text = { Text("复制全部代码", style = CaptionStyle) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("snippet", codeContent)
                            clipboard.setPrimaryClip(clip)
                            onShowSnackbar(context.getString(R.string.toast_copied))
                        }
                    )

                    // 立即强制保存
                    DropdownMenuItem(
                        text = { Text("立即保存", style = CaptionStyle) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_save),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onForceSave()
                            onShowSnackbar("已保存")
                        }
                    )

                    // Git 历史履历入口
                    if (snippetId != "new" && onNavigateToHistory != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_git_history), style = CaptionStyle) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_git),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onNavigateToHistory(snippetId)
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg)
    )
}
