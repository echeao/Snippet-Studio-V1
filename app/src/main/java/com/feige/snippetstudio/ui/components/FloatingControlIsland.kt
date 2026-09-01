package com.feige.snippetstudio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [FloatingControlIsland] 全屏沉浸模式下的悬浮控制岛组件。
 *
 * 现代化交互升级亮点：
 * 1. **双态折叠架构**：支持完整展开态（Expanded）与极简微标态（Collapsed），避免长期遮挡代码核心内容。
 * 2. **极简微标态**：折叠后仅保留小巧半透明药丸徽标，停靠不挡视线，点击即恢复完整面板。
 * 3. **全功能快捷支持**：提供【代码/预览】切换、【符号输入栏】开关、【一键退出全屏】与【收起折叠】。
 * 4. **视觉与质感提升**：采用半透明毛玻璃表面、柔和阴影与精致圆角，适配暗黑/浅色主题。
 *
 * @param selectedTab 当前选中的 Tab (0: 代码, 1: 预览)
 * @param onSelectTab 切换 Tab 回调
 * @param showSymbolBar 符号栏是否开启显示
 * @param onToggleSymbolBar 切换符号栏显示回调
 * @param onExitFullscreen 退出全屏回调
 * @param isExpanded 控制岛是否处于展开状态
 * @param onToggleExpand 切换展开/折叠状态回调
 * @param modifier 外部修饰符（支持拖拽偏移与内外边距）
 */
@Composable
fun FloatingControlIsland(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    showSymbolBar: Boolean,
    onToggleSymbolBar: () -> Unit,
    onExitFullscreen: () -> Unit,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .shadow(AppElevation.Md, RoundedCornerShape(R_XL), ambientColor = AppElevation.MdColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_XL))
            .testTag("floating_control_island"),
        shape = RoundedCornerShape(R_XL),
        color = tc.surface.copy(alpha = 0.94f)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FloatingIslandStateTransition"
        ) { expanded ->
            if (expanded) {
                // ===== 状态 A：完整展开控制面板 =====
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    // 1. [代码 / 预览] Tab 切换胶囊
                    Row(
                        modifier = Modifier
                            .background(tc.surface2, RoundedCornerShape(R_MD))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (selectedTab == 0) tc.primary else Color.Transparent,
                            shape = RoundedCornerShape(R_SM),
                            modifier = Modifier.clickable { onSelectTab(0) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_code),
                                    contentDescription = "Code",
                                    tint = if (selectedTab == 0) Color.White else tc.text2,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "代码",
                                    style = CaptionStyle,
                                    color = if (selectedTab == 0) Color.White else tc.text2
                                )
                            }
                        }

                        Surface(
                            color = if (selectedTab == 1) tc.primary else Color.Transparent,
                            shape = RoundedCornerShape(R_SM),
                            modifier = Modifier.clickable { onSelectTab(1) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_visibility),
                                    contentDescription = "Preview",
                                    tint = if (selectedTab == 1) Color.White else tc.text2,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "预览",
                                    style = CaptionStyle,
                                    color = if (selectedTab == 1) Color.White else tc.text2
                                )
                            }
                        }
                    }

                    // 2. 符号栏切换按钮（仅在代码模式显示）
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = onToggleSymbolBar,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_code),
                                contentDescription = "Symbol Bar Toggle",
                                tint = if (showSymbolBar) tc.primary else tc.text2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.height(18.dp),
                        color = tc.line
                    )

                    // 3. 退出全屏按钮
                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_minimize),
                            contentDescription = "Exit Fullscreen",
                            tint = Danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 4. 折叠收起按钮（缩减为微标，释放代码视野）
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Collapse Island",
                            tint = tc.text2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // ===== 状态 B：极简紧凑微标态（仅占极小区域，点击展开）=====
                Row(
                    modifier = Modifier
                        .clickable { onToggleExpand() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (selectedTab == 0) R.drawable.ic_code else R.drawable.ic_visibility
                        ),
                        contentDescription = "Control Island Mode",
                        tint = tc.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (selectedTab == 0) "代码" else "预览",
                        style = CaptionStyle,
                        fontWeight = FontWeight.Medium,
                        color = tc.text
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                        contentDescription = "Expand Island",
                        tint = tc.text2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
