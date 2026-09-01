package com.feige.snippetstudio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * [AppElevation] 全局卡片悬浮高程与柔和环境光阴影规范。
 * 避免粗重生硬的黑色扩散，采用超低透明度的现代柔和光影体系。
 */
object AppElevation {
    /** 浅层柔和卡片阴影 (用于普通列表卡片、搜索框等) */
    val Sm = 4.dp
    val SmColor = Color(0x08000000)

    /** 中层浮动阴影 (用于悬浮胶囊底栏、全屏控制岛、弹窗等) */
    val Md = 10.dp
    val MdColor = Color(0x12000000)

    /** 强层突出阴影 (用于浮动操作按键 FAB 等) */
    val Lg = 16.dp
    val LgColor = Color(0x18000000)
}

