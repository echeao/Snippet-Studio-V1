package com.feige.snippetstudio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * [Color.kt] 定义了 Snippet Studio 应用全局的色彩体系与设计 Tokens。
 *
 * 色彩风格：Natural Organic & Modern Slate (自然有机大地色与现代墨绿灰组合)。
 * 避免生硬纯纯色，采用精心搭配的低饱和度、高质感 HSL 调色板。
 */

// ===== 品牌与核心主色 (Primary Accent Colors) =====
val Primary = Color(0xFF4B635A)      // 深沉石板森林绿 (主色调)
val Primary2 = Color(0xFF717E68)     // 柔滑鼠尾草绿 (次要主色)
val PrimarySoft = Color(0xFFE7EDDE)  // 浅鼠尾草色容器背景
val PrimaryLine = Color(0xFFC8D3C3)  // 鼠尾草色边框强调线

// ===== 浅色主题色彩 (Light Theme Palette) =====
val BgLight = Color(0xFFFDF8F3)      // 温暖自然有机米白色背景
val SurfaceLight = Color(0xFFFFFFFF) // 白色卡片与表面容器
val Surface2Light = Color(0xFFF2F0E9)// 软糯暖调容器背景
val TextLight = Color(0xFF1C1B1F)    // 深大地黑主要文字
val Text2Light = Color(0xFF484944)   // 暗橄榄绿次要文字
val Text3Light = Color(0xFF797871)   // 暖灰弱化说明文字
val LineLight = Color(0xFFE5E2D9)    // 自然柔和分割线

// ===== 深色主题色彩 (Dark Theme Palette) =====
val BgDark = Color(0xFF171B18)       // 深邃石板森林绿背景
val SurfaceDark = Color(0xFF202521)  // 暖深石板卡片表面
val Surface2Dark = Color(0xFF2B322D) // 石板暗绿容器背景
val TextDark = Color(0xFFF0F2ED)     // 柔暖浅绿主要文字
val Text2Dark = Color(0xFFBCC5B8)    // 鼠尾草灰次要文字
val Text3Dark = Color(0xFF879283)    // 弱化绿灰文字
val LineDark = Color(0xFF38413B)     // 深色自然边框线

// ===== 功能反馈状态色 (Functional Alert Colors) =====
val Success = Color(0xFF3A6B4C)      // 成功绿
val SuccessSoft = Color(0xFFE8F2EC)  // 成功浅绿背景
val Warning = Color(0xFF9E6723)      // 警告橙棕
val WarningSoft = Color(0xFFFBF2E7)  // 警告浅背景
val Danger = Color(0xFFA8423F)       // 危险红
val DangerSoft = Color(0xFFFAECEB)   // 危险浅红背景

// ===== 代码片段类型与分类专属配对色 (Category & Type Colors) =====
val C_Html = Color(0xFFB4533C)       // HTML 标签橙红
val C_HtmlBg = Color(0xFFF9EFEA)

val C_Js = Color(0xFF8F752C)         // JavaScript 芥末金黄
val C_JsBg = Color(0xFFFAF5E8)

val C_Md = Color(0xFF3B6B78)         // Markdown 墨青蓝
val C_MdBg = Color(0xFFEBF3F5)

val C_Prompt = Color(0xFF4B635A)     // Prompt 提示词森林绿
val C_PromptBg = Color(0xFFE7EDDE)

val C_Store = Color(0xFF4A657A)      // 存储指示色
val C_StoreBg = Color(0xFFECF2F7)

val C_Sync = Color(0xFF3A6B4C)       // 同步指示色
val C_SyncBg = Color(0xFFE8F2EC)

val C_Org = Color(0xFF4B635A)        // 架构分类指示色
val C_OrgBg = Color(0xFFE7EDDE)

val C_Tag = Color(0xFF9E5669)        // 标签浅莓红
val C_TagBg = Color(0xFFF9EFF2)

val C_Backup = Color(0xFF546C5B)     // 备份状态绿
val C_BackupBg = Color(0xFFEDF3EE)

val C_Look = Color(0xFF5C5E5B)       // 外观指示中性灰
val C_LookBg = Color(0xFFF0F0EE)

val StarOn = Color(0xFFD4A237)       // 收藏金黄星标色

// ===== 语法高亮词法单元色彩 (Syntax Highlighting Tokens) =====
val Tk_Tag = Color(0xFF4B635A)       // 标签词法
val Tk_Attr = Color(0xFF3B6B78)      // 属性词法
val Tk_Str = Color(0xFF3A6B4C)       // 字符串
val Tk_Com = Color(0xFF797871)       // 注释
val Tk_Kw = Color(0xFF9E6723)        // 关键字
val Tk_Num = Color(0xFF8F752C)       // 数字
val Tk_Punc = Color(0xFF5C5E5B)      // 标点符号

