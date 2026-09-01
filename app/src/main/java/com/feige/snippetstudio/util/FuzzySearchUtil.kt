package com.feige.snippetstudio.util

/**
 * [FuzzySearchUtil] 模糊搜索与多关键词子序列匹配工具类。
 *
 * 核心算法逻辑：
 * 1. 支持按空格拆分为多个独立关键词，要求所有关键词均命中目标文本（AND 逻辑）。
 * 2. 对单关键词使用子序列匹配（字符按顺序在文本中出现即可，无需连续），并启用忽略大小写。
 * 3. 使用原生 `indexOf(char, startIndex, ignoreCase = true)`，避免对大型代码片段正文全文执行 `lowercase()` 分配，大幅减轻 GC 内存压力。
 */
object FuzzySearchUtil {

    /** 预编译空格分词正则表达式，避免高频搜索时重复创建 Regex 实例 */
    private val WHITESPACE_REGEX = Regex("\\s+")

    /**
     * 检验目标文本 [text] 是否匹配搜索查询 [query]。
     *
     * @param text 搜索目标文本（标题、正文或标签）
     * @param query 用户输入的搜索词
     * @return true 表示全部关键词均匹配通过
     */
    fun match(text: String, query: String): Boolean {
        if (query.isBlank()) return true
        val keywords = query.trim().split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        return keywords.all { keyword ->
            containsSubsequence(text, keyword)
        }
    }

    /**
     * 判断 [query] 字符子序列是否按顺序存在于 [text] 中（忽略大小写）。
     *
     * @param text 待查找的长文本
     * @param query 关键词子串
     * @return 是否包含对应子序列
     */
    private fun containsSubsequence(text: String, query: String): Boolean {
        if (query.isEmpty()) return true
        var ti = 0
        val textLength = text.length
        for (i in 0 until query.length) {
            val qc = query[i]
            ti = text.indexOf(qc, ti, ignoreCase = true)
            if (ti < 0 || ti >= textLength) return false
            ti++
        }
        return true
    }
}
