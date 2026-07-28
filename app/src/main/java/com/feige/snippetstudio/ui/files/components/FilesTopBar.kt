package com.feige.snippetstudio.ui.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.files.DensityMode
import com.feige.snippetstudio.ui.files.SortMode
import com.feige.snippetstudio.ui.files.ViewMode
import com.feige.snippetstudio.ui.theme.*

/**
 * [FilesTopBar] 文件中心顶栏交互组件。
 *
 * 职责说明：
 * 1. 展示文件中心应用大标题 [DisplayTitleStyle]。
 * 2. 提供四大核心控制功能按钮：
 *    - 【新建文件夹】：触发文件夹新建弹窗。
 *    - 【显示密度】：在 COMFORT 大卡片与 COMPACT 高密度模式之间切换。
 *    - 【视图结构】：在 FLAT 平铺与 TREE 树状视图之间切换。
 *    - 【排序规则】：弹出下拉菜单选择修改时间/名称/类型排序。
 * 3. 使用 [LocalThemeColors] 配色与选中文感微高亮，确保精美的视觉质感。
 *
 * @param sortMode 当前排序模式
 * @param viewMode 当前视图结构模式
 * @param densityMode 当前显示密度模式
 * @param onCreateFolderClick 点击新建文件夹回调
 * @param onToggleDensityClick 点击切换显示密度回调
 * @param onToggleViewModeClick 点击切换视图模式回调
 * @param onSelectSortMode 选中特定排序规则回调
 * @param modifier 外部修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTopBar(
    sortMode: SortMode,
    viewMode: ViewMode,
    densityMode: DensityMode,
    onCreateFolderClick: () -> Unit,
    onToggleDensityClick: () -> Unit,
    onToggleViewModeClick: () -> Unit,
    onSelectSortMode: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.files_title),
                style = DisplayTitleStyle,
                color = tc.text
            )
        },
        actions = {
            // ===== 按钮 0: 新建文件夹按钮 =====
            IconButton(
                onClick = onCreateFolderClick,
                modifier = Modifier.testTag("files_create_folder_btn")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_plus),
                    contentDescription = "Create Folder",
                    tint = tc.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ===== 按钮 1: 切换【大卡片 / 高密度】显示密度模式 =====
            IconButton(
                onClick = onToggleDensityClick,
                modifier = Modifier
                    .testTag("files_density_mode_btn")
                    .clip(RoundedCornerShape(R_SM))
                    .background(if (densityMode == DensityMode.COMPACT) tc.primary.copy(alpha = 0.12f) else Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(id = if (densityMode == DensityMode.COMFORT) R.drawable.ic_list else R.drawable.ic_grid),
                    contentDescription = "Toggle Density Mode",
                    tint = tc.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ===== 按钮 2: 切换【平铺 / 树状】视图结构 =====
            IconButton(
                onClick = onToggleViewModeClick,
                modifier = Modifier
                    .testTag("files_view_mode_btn")
                    .clip(RoundedCornerShape(R_SM))
                    .background(if (viewMode == ViewMode.TREE) tc.primary.copy(alpha = 0.12f) else Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(id = if (viewMode == ViewMode.FLAT) R.drawable.ic_tree else R.drawable.ic_treetolist),
                    contentDescription = "Toggle View Mode",
                    tint = tc.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ===== 按钮 3: 排序规则下拉弹出菜单 =====
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("files_sort_btn")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sort),
                        contentDescription = "Sort Options",
                        tint = tc.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(tc.surface)
                ) {
                    // 选项 1: 按修改时间降序
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sort_updated),
                                color = if (sortMode == SortMode.UPDATED_DESC) tc.primary else tc.text,
                                style = BodyStyle
                            )
                        },
                        onClick = {
                            onSelectSortMode(SortMode.UPDATED_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = if (sortMode == SortMode.UPDATED_DESC) {
                            {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = tc.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    // 选项 2: 按片段名称升序
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sort_name),
                                color = if (sortMode == SortMode.NAME_ASC) tc.primary else tc.text,
                                style = BodyStyle
                            )
                        },
                        onClick = {
                            onSelectSortMode(SortMode.NAME_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = if (sortMode == SortMode.NAME_ASC) {
                            {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = tc.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    // 选项 3: 按类型升序
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sort_type),
                                color = if (sortMode == SortMode.TYPE_ASC) tc.primary else tc.text,
                                style = BodyStyle
                            )
                        },
                        onClick = {
                            onSelectSortMode(SortMode.TYPE_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = if (sortMode == SortMode.TYPE_ASC) {
                            {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = tc.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg),
        modifier = modifier
    )
}
