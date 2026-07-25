package com.feige.snippetstudio.util

import com.feige.snippetstudio.model.PromptVariable

/**
 * [PromptVariableParser] 是 Prompt 模板变量解析引擎。
 *
 * 核心功能：
 * 1. 从 Prompt 文本中提取所有 `{{变量名}}` 或 `{{变量名:默认值}}` 格式的占位符。
 * 2. 对同名变量进行去重合并，记录每个变量在原文中的所有出现位置。
 * 3. 提供批量替换方法，将变量占位符替换为用户填充的实际值。
 */
object PromptVariableParser {

    /**
     * 匹配 `{{变量名}}` 或 `{{变量名:默认值}}` 的正则表达式。
     * - 变量名：支持中文、英文字母、数字、下划线
     * - 默认值：冒号后的任意非 `}` 字符
     */
    private val VARIABLE_PATTERN = Regex("\\{\\{([\\w\\u4e00-\\u9fa5]+)(?::([^}]*))?\\}\\}")

    /**
     * 解析文本中的所有 Prompt 变量占位符。
     *
     * @param text Prompt 模板原文
     * @return 去重后的变量列表，每个变量包含所有出现位置
     */
    fun parse(text: String): List<PromptVariable> {
        if (text.isBlank()) return emptyList()

        val variableMap = LinkedHashMap<String, MutableList<IntRange>>()
        val defaultValues = HashMap<String, String>()

        VARIABLE_PATTERN.findAll(text).forEach { match ->
            val name = match.groupValues[1]
            val defaultValue = match.groupValues.getOrNull(2) ?: ""
            val range = match.range

            variableMap.getOrPut(name) { mutableListOf() }.add(range)
            // 记录首次出现的默认值
            if (defaultValue.isNotBlank() && !defaultValues.containsKey(name)) {
                defaultValues[name] = defaultValue
            }
        }

        return variableMap.map { (name, ranges) ->
            PromptVariable(
                name = name,
                defaultValue = defaultValues[name] ?: "",
                occurrences = ranges
            )
        }
    }

    /**
     * 将文本中的变量占位符批量替换为对应的填充值。
     *
     * @param text 原始 Prompt 模板文本
     * @param values 变量名 -> 填充值的映射
     * @return 替换后的完整文本
     */
    fun fill(text: String, values: Map<String, String>): String {
        return VARIABLE_PATTERN.replace(text) { match ->
            val name = match.groupValues[1]
            val defaultValue = match.groupValues.getOrNull(2) ?: ""
            values[name]?.ifBlank { defaultValue } ?: defaultValue.ifBlank { match.value }
        }
    }

    /**
     * 快速检测文本中是否包含变量占位符。
     */
    fun hasVariables(text: String): Boolean {
        return VARIABLE_PATTERN.containsMatchIn(text)
    }
}
