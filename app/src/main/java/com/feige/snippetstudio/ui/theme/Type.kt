package com.feige.snippetstudio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 大标题字体样式 (如主页顶栏标题) */
val DisplayTitleStyle = TextStyle(
    fontSize = 25.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 32.sp
)

/** 分组/章节标题字体样式 */
val SectionTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 22.sp
)

/** 列表项标题字体样式 */
val ListTitleStyle = TextStyle(
    fontSize = 15.5.sp,
    fontWeight = FontWeight.W800,
    lineHeight = 20.sp
)

/** 标准正文文本样式 */
val BodyStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.W400,
    lineHeight = 21.sp
)

/** 辅助说明文字文本样式 */
val CaptionStyle = TextStyle(
    fontSize = 12.5.sp,
    fontWeight = FontWeight.W700,
    lineHeight = 16.sp
)

/** 徽章/胶囊标签文字样式 */
val BadgeStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.W900,
    lineHeight = 14.sp
)

/** 等宽代码字体样式 */
val CodeTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.5.sp,
    fontWeight = FontWeight.W400,
    lineHeight = 22.sp
)

/**
 * [AppTypography] Material 3 体系的全局字体映射配置。
 */
val AppTypography = Typography(
    displayLarge = DisplayTitleStyle,
    titleLarge = SectionTitleStyle,
    titleMedium = ListTitleStyle,
    bodyLarge = BodyStyle,
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelMedium = CaptionStyle,
    labelSmall = BadgeStyle
)

