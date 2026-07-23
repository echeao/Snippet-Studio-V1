package com.feige.snippetstudio.util

import android.content.ClipboardManager
import android.content.Context
import com.feige.snippetstudio.model.SnippetType

data class DetectedClip(
    val content: String,
    val previewText: String,
    val inferredType: SnippetType,
    val contentHash: Int
)

object ClipboardDetector {
    private var ignoredHashes = mutableSetOf<Int>()

    fun ignore(hash: Int) {
        ignoredHashes.add(hash)
    }

    fun detect(context: Context): DetectedClip? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null || !clipboard.hasPrimaryClip()) return null

            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim() ?: return null

            if (text.isBlank()) return null

            val hash = text.hashCode()
            if (ignoredHashes.contains(hash)) return null

            val inferredType = inferType(text)
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

    private fun inferType(text: String): SnippetType {
        return when {
            text.contains("<") && text.contains(">") -> SnippetType.HTML
            text.contains(Regex("function|const |let |=>|console\\.")) -> SnippetType.JS
            text.contains(Regex("(^#|\\*\\*|^\\s*- )", RegexOption.MULTILINE)) -> SnippetType.MARKDOWN
            else -> SnippetType.PROMPT
        }
    }
}
