package com.feige.snippetstudio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayTitleStyle = TextStyle(
    fontSize = 25.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 32.sp
)

val SectionTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 22.sp
)

val ListTitleStyle = TextStyle(
    fontSize = 15.5.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 20.sp
)

val BodyStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.W400,
    lineHeight = 21.sp
)

val CaptionStyle = TextStyle(
    fontSize = 12.5.sp,
    fontWeight = FontWeight.W700,
    lineHeight = 16.sp
)

val BadgeStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.W900,
    lineHeight = 14.sp
)

val CodeTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.5.sp,
    fontWeight = FontWeight.W400,
    lineHeight = 22.sp
)

val AppTypography = Typography(
    displayLarge = DisplayTitleStyle,
    titleLarge = SectionTitleStyle,
    titleMedium = ListTitleStyle,
    bodyLarge = BodyStyle,
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelMedium = CaptionStyle,
    labelSmall = BadgeStyle
)
