package com.feige.snippetstudio.util

import android.content.ClipboardManager
import android.content.Context
import com.feige.snippetstudio.model.SnippetType

/**
 * [DetectedClip] 封装从系统剪贴板中检测到的有效文本块元数据。
 *
 * @param content 剪贴板完整文本正文
 * @param previewText UI 上显示的缩略文本预览
 * @param inferredType 智能推断的代码片段类型 [SnippetType]
 * @param contentHash 文本内容的唯一哈希值，用于去重和防重复弹出
 */
data class DetectedClip(
    val content: String,
    val previewText: String,
    val inferredType: SnippetType,
    val contentHash: Int
)

/**
 * [ClipboardDetector] 剪贴板内容智能检测器。
 *
 * 核心逻辑：当应用切回前台时，自动侦测系统剪贴板中的代码/文本，识别其类型并弹出“一键生成片段”快捷提示条。
 */
object ClipboardDetector {
    /** 存储已被用户选择忽略的剪贴板文本哈希集合，防止用户关闭弹窗后反复提示 */
    private var ignoredHashes = mutableSetOf<Int>()

    /**
     * 将指定文本哈希加入忽略集合。
     */
    fun ignore(hash: Int) {
        ignoredHashes.add(hash)
    }

    /**
     * 检查当前系统剪贴板中的文本。
     *
     * @param context 上下文对象
     * @return 若找到未被忽略的非空有效文本，则返回 [DetectedClip]，否则返回 null
     */
    /**
     * 检查当前系统剪贴板中的文本。
     *
     * 教学解析：
     * 1. 结合安卓 `CLIPBOARD_SERVICE` 获取系统剪贴板管理器。
     * 2. `text.hashCode()` 去重逻辑：使用 String 散列码与 `ignoredHashes` 集合比对，
     *    当用户点击“取消/忽略”提示条时将 hash 沉淀到集合中，避免每次应用切回前台都弹出相同的提示。
     *
     * @param context 上下文对象
     * @return 若找到未被忽略的非空有效文本，则返回 [DetectedClip]，否则返回 null
     */
    fun detect(context: Context): DetectedClip? {
        return try {
            // 获取 Android 系统剪贴板服务对象
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null || !clipboard.hasPrimaryClip()) return null

            // 提取主剪贴板的第 0 项文本内容
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim() ?: return null

            if (text.isBlank()) return null

            // 计算该段文字的哈希码以去重
            val hash = text.hashCode()
            if (ignoredHashes.contains(hash)) return null

            // 根据字符特征自动推导语言类型
            val inferredType = inferType(text)
            // 生成首行/前 40 字符的缩略文案用于 UI 顶部预览
            val preview = if (text.length > 40) text.take(40) + "…" else text

            DetectedClip(
                content = text,
                previewText = preview,
                inferredType = inferredType,
                contentHash = hash
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 根据文本特征正则表达式自动推断代码片段类型 (HTML / JS / Markdown / Prompt)。
     *
     * 匹配规则：
     * - HTML: 包含尖括号 `<` 与 `>` (如 `<div...`)
     * - JS: 包含关键字 `function`, `const`, `let`, 箭头函数 `=>` 或 `console.`
     * - Markdown: 多行正则匹配以 `#` 标题开头的行、`**` 加粗符号或 `- ` 无序列表
     * - 默认 fallback: 归类为 Prompt AI 提示词
     */
    private fun inferType(text: String): SnippetType {
        return when {
            text.contains("<") && text.contains(">") -> SnippetType.HTML
            text.contains(Regex("\\b(public\\s+class|package\\s+[a-zA-Z0-9_.]+|import\\s+java|System\\.out\\.print)\\b")) -> SnippetType.JAVA
            text.contains(Regex("function|const |let |=>|console\\.")) -> SnippetType.JS
            text.contains(Regex("(^#|\\*\\*|^\\s*- )", RegexOption.MULTILINE)) -> SnippetType.MARKDOWN
            else -> SnippetType.PROMPT
        }
    }
}


