package com.feige.snippetstudio.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FuzzySearchUtilTest] 针对 [FuzzySearchUtil] 的单元测试套件。
 *
 * 覆盖测试场景：
 * 1. 空搜索词或纯空格的边界返回
 * 2. 精确全词匹配与大小写不敏感匹配
 * 3. 字符非连续子序列匹配（如 "hw" 匹配 "Hello World"）
 * 4. 多关键词（空格分隔）AND 组合匹配
 * 5. 中文与混合字符搜索
 */
class FuzzySearchUtilTest {

    @Test
    fun `test match returns true for empty or blank query`() {
        assertTrue(FuzzySearchUtil.match("Any text here", ""))
        assertTrue(FuzzySearchUtil.match("Any text here", "   "))
    }

    @Test
    fun `test match exact and case-insensitive substrings`() {
        val text = "Jetpack Compose Snippet Studio"
        assertTrue(FuzzySearchUtil.match(text, "compose"))
        assertTrue(FuzzySearchUtil.match(text, "COMPOSE"))
        assertTrue(FuzzySearchUtil.match(text, "Snippet"))
        assertFalse(FuzzySearchUtil.match(text, "Flutter"))
    }

    @Test
    fun `test match subsequence in order`() {
        val text = "RecyclerViewAdapter.kt"
        // 子序列按序匹配
        assertTrue(FuzzySearchUtil.match(text, "rva"))
        assertTrue(FuzzySearchUtil.match(text, "rcv"))
        // 逆序字符不应匹配
        assertFalse(FuzzySearchUtil.match(text, "avr"))
    }

    @Test
    fun `test match multiple keywords with AND logic`() {
        val text = "Quick brown fox jumps over the lazy dog"
        // 包含所有关键词
        assertTrue(FuzzySearchUtil.match(text, "fox dog"))
        assertTrue(FuzzySearchUtil.match(text, "quick lazy"))
        // 只要有一个关键词未命中，即返回 false
        assertFalse(FuzzySearchUtil.match(text, "fox cat"))
    }

    @Test
    fun `test match chinese characters and mixed text`() {
        val text = "提示词模板：生成 Kotlin 单元测试"
        assertTrue(FuzzySearchUtil.match(text, "提示词"))
        assertTrue(FuzzySearchUtil.match(text, "kotlin 测试"))
        assertTrue(FuzzySearchUtil.match(text, "kt 测试"))
        assertFalse(FuzzySearchUtil.match(text, "Python 测试"))
    }
}
