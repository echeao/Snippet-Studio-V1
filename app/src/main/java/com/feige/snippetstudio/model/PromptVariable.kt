package com.feige.snippetstudio.model

import androidx.compose.runtime.Immutable

/**
 * [PromptVariable] 表示 Prompt 模板中检测到的一个动态变量占位符。
 *
 * 支持格式：`{{变量名}}` 或 `{{变量名:默认值}}`
 *
 * @param name 变量名称（如 "language"、"topic"）
 * @param defaultValue 变量默认值（从 `{{name:default}}` 格式中解析，无则为空字符串）
 * @param occurrences 该变量在原文中所有出现位置的字符区间列表（用于批量替换）
 */
@Immutable
data class PromptVariable(
    val name: String,
    val defaultValue: String = "",
    val occurrences: List<IntRange> = emptyList()
)
