package com.feige.snippetstudio.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PromptVariableParserTest] 针对 [PromptVariableParser] 的单元测试套件。
 *
 * 覆盖测试场景：
 * 1. 无变量普通文本检测与解析
 * 2. 基础 `{{var}}` 格式提取
 * 3. 带默认值 `{{var:default}}` 提取
 * 4. 同一变量多次出现的位置记录与去重
 * 5. 中文与特殊变量名支持
 * 6. 批量变量填充与默认值回退
 */
class PromptVariableParserTest {

    @Test
    fun `test hasVariables with and without placeholders`() {
        assertFalse(PromptVariableParser.hasVariables("This is a plain prompt without variables."))
        assertFalse(PromptVariableParser.hasVariables("Single brace {not_a_variable}"))
        assertTrue(PromptVariableParser.hasVariables("Hello {{name}}, welcome to {{city:Beijing}}!"))
        assertTrue(PromptVariableParser.hasVariables("你好 {{用户}}，今天是 {{日期}}"))
    }

    @Test
    fun `test parse returns empty list for blank text`() {
        assertTrue(PromptVariableParser.parse("").isEmpty())
        assertTrue(PromptVariableParser.parse("   ").isEmpty())
        assertTrue(PromptVariableParser.parse("No variables here").isEmpty())
    }

    @Test
    fun `test parse extracts variables with and without default values`() {
        val text = "Translate this into {{language:English}}: {{content}}"
        val vars = PromptVariableParser.parse(text)

        assertEquals(2, vars.size)

        val langVar = vars.first { it.name == "language" }
        assertEquals("language", langVar.name)
        assertEquals("English", langVar.defaultValue)
        assertEquals(1, langVar.occurrences.size)

        val contentVar = vars.first { it.name == "content" }
        assertEquals("content", contentVar.name)
        assertEquals("", contentVar.defaultValue)
        assertEquals(1, contentVar.occurrences.size)
    }

    @Test
    fun `test parse deduplicates repeated variables and records occurrences`() {
        val text = "User {{username}} likes {{username}}. Contact {{username:admin@test.com}}."
        val vars = PromptVariableParser.parse(text)

        assertEquals(1, vars.size)
        val userVar = vars[0]
        assertEquals("username", userVar.name)
        // 首次出现的默认值为空，后续带默认值不覆盖已记录的
        assertEquals(3, userVar.occurrences.size)
    }

    @Test
    fun `test fill with user values replaces placeholders properly`() {
        val template = "Dear {{name:Customer}}, your order {{order_id}} is ready for {{action:pickup}}."
        val values = mapOf(
            "name" to "Alice",
            "order_id" to "ORD-12345"
        )

        val result = PromptVariableParser.fill(template, values)
        // action 未提供，使用默认值 pickup
        assertEquals("Dear Alice, your order ORD-12345 is ready for pickup.", result)
    }

    @Test
    fun `test fill with blank values falls back to default values or retains placeholder`() {
        val template = "Format: {{format:JSON}}, Topic: {{topic}}"
        val values = mapOf(
            "format" to "", // 空白值应当回退到默认值 JSON
            "topic" to ""   // 无默认值且提供空值，保留占位符或空
        )

        val result = PromptVariableParser.fill(template, values)
        assertEquals("Format: JSON, Topic: {{topic}}", result)
    }
}
