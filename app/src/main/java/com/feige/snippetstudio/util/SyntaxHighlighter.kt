package com.feige.snippetstudio.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.feige.snippetstudio.model.SnippetType
import java.util.regex.Pattern

object SyntaxHighlighter {

    // Token colors
    private fun getKeywordStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFC792EA) else Color(0xFF7B1FA2),
        fontWeight = FontWeight.Bold
    )

    private fun getStringStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFC3E88D) else Color(0xFF2E7D32)
    )

    private fun getNumberStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFF78C6C) else Color(0xFFE65100)
    )

    private fun getCommentStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF78909C) else Color(0xFF757575),
        fontStyle = FontStyle.Italic
    )

    private fun getTagStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B),
        fontWeight = FontWeight.SemiBold
    )

    private fun getAttrStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFFCB6B) else Color(0xFFF57F17)
    )

    private fun getHeaderStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF82AAFF) else Color(0xFF1565C0),
        fontWeight = FontWeight.Bold
    )

    private fun getVariableStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFF5370) else Color(0xFFD81B60),
        fontWeight = FontWeight.Bold
    )

    // Regex Patterns
    private val JS_KEYWORD_PATTERN = Pattern.compile(
        "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|import|export|from|default|class|extends|async|await|try|catch|finally|throw|new|this|typeof|instanceof|void|in|of|null|undefined|true|false)\\b"
    )
    private val JS_STRING_PATTERN = Pattern.compile(
        "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`"
    )
    private val NUMBER_PATTERN = Pattern.compile(
        "\\b\\d+(\\.\\d+)?\\b"
    )
    private val JS_COMMENT_PATTERN = Pattern.compile(
        "//.*|/\\*[\\s\\S]*?\\*/"
    )

    private val HTML_TAG_PATTERN = Pattern.compile(
        "</?[a-zA-Z0-9\\-]+(?:\\s+[a-zA-Z0-9\\-]+(?:=(?:\"[^\"]*\"|'[^']*'|[^>\\s]+))?)*\\s*/?>"
    )
    private val HTML_COMMENT_PATTERN = Pattern.compile(
        "<!--[\\s\\S]*?-->"
    )
    private val HTML_ATTR_NAME_PATTERN = Pattern.compile(
        "\\b[a-zA-Z0-9\\-]+(?=\\=)"
    )

    private val MD_HEADER_PATTERN = Pattern.compile(
        "(?m)^#{1,6}\\s+.*$"
    )
    private val MD_BOLD_PATTERN = Pattern.compile(
        "\\*\\*.*?\\*\\*|__.*?__"
    )
    private val MD_CODE_PATTERN = Pattern.compile(
        "`[^`]+`"
    )

    private val PROMPT_VAR_PATTERN = Pattern.compile(
        "\\{\\{?[a-zA-Z0-9_]+\\}?\\}|\\$[a-zA-Z0-9_]+"
    )

    fun highlight(text: String, type: SnippetType, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            append(text)

            when (type) {
                SnippetType.JS -> highlightJs(text, isDark)
                SnippetType.HTML -> highlightHtml(text, isDark)
                SnippetType.MARKDOWN -> highlightMarkdown(text, isDark)
                SnippetType.PROMPT -> highlightPrompt(text, isDark)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightJs(text: String, isDark: Boolean) {
        // Keywords
        val kwMatcher = JS_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // Numbers
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // Strings
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // Comments (override others)
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightHtml(text: String, isDark: Boolean) {
        // HTML Tags
        val tagMatcher = HTML_TAG_PATTERN.matcher(text)
        val tagStyle = getTagStyle(isDark)
        while (tagMatcher.find()) {
            addStyle(tagStyle, tagMatcher.start(), tagMatcher.end())
        }

        // Attr names
        val attrMatcher = HTML_ATTR_NAME_PATTERN.matcher(text)
        val attrStyle = getAttrStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, attrMatcher.start(), attrMatcher.end())
        }

        // Strings
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // HTML Comments
        val cmtMatcher = HTML_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightMarkdown(text: String, isDark: Boolean) {
        // Headers
        val headerMatcher = MD_HEADER_PATTERN.matcher(text)
        val headerStyle = getHeaderStyle(isDark)
        while (headerMatcher.find()) {
            addStyle(headerStyle, headerMatcher.start(), headerMatcher.end())
        }

        // Bold
        val boldMatcher = MD_BOLD_PATTERN.matcher(text)
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        while (boldMatcher.find()) {
            addStyle(boldStyle, boldMatcher.start(), boldMatcher.end())
        }

        // Code
        val codeMatcher = MD_CODE_PATTERN.matcher(text)
        val codeStyle = getStringStyle(isDark)
        while (codeMatcher.find()) {
            addStyle(codeStyle, codeMatcher.start(), codeMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightPrompt(text: String, isDark: Boolean) {
        // Prompt Variables like {var}, {{var}}, $var
        val varMatcher = PROMPT_VAR_PATTERN.matcher(text)
        val varStyle = getVariableStyle(isDark)
        while (varMatcher.find()) {
            addStyle(varStyle, varMatcher.start(), varMatcher.end())
        }
    }
}
