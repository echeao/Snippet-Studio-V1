package com.feige.snippetstudio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val R_XL = 26.dp
val R_LG = 20.dp
val R_MD = 16.dp
val R_SM = 12.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(R_SM),
    medium = RoundedCornerShape(R_MD),
    large = RoundedCornerShape(R_LG),
    extraLarge = RoundedCornerShape(R_XL)
)
