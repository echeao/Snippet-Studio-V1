package com.feige.snippetstudio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 全局圆角大小定义规范（特大 28dp、大 20dp、中 14dp、小 8dp）。
 */
val R_XL = 28.dp
val R_LG = 20.dp
val R_MD = 14.dp
val R_SM = 8.dp

/**
 * [AppShapes] 统一的 Material 3 形状体系。
 */
val AppShapes = Shapes(
    small = RoundedCornerShape(R_SM),
    medium = RoundedCornerShape(R_MD),
    large = RoundedCornerShape(R_LG),
    extraLarge = RoundedCornerShape(R_XL)
)

