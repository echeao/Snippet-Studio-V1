package com.feige.snippetstudio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import java.io.IOException
import java.io.InputStream

/**
 * [SharedFileResult] 表示解析系统分享文件意图后的结果。
 *
 * - [Success]：文件成功读取，包含文件名、内容、推断类型与字节大小。
 * - [Error]：解析失败，携带对应的多语言字符串资源 ID 用于 Toast 提示。
 */
sealed class SharedFileResult {
    data class Success(
        val fileName: String,
        val content: String,
        val detectedType: SnippetType,
        val sizeBytes: Int
    ) : SharedFileResult()

    data class Error(val messageResId: Int) : SharedFileResult()
}

/**
 * [SharedFileHandler] 负责解析系统 ACTION_SEND 分享的文件 Intent，
 * 从 content:// URI 中读取文本/代码文件内容并推断片段类型。
 *
 * 核心职责：
 * 1. 从 Intent 的 EXTRA_STREAM 中提取 content:// URI。
 * 2. 通过 ContentResolver 查询文件名 (DISPLAY_NAME) 与文件大小 (SIZE)。
 * 3. 校验文件扩展名是否在支持的文本/代码白名单内。
 * 4. 以 UTF-8 编码读取文件内容为 String（自动跳过 BOM）。
 * 5. 根据文件扩展名推断 [SnippetType] 业务分类。
 *
 * 限制：
 * - 仅支持单文件 (ACTION_SEND)，不支持 ACTION_SEND_MULTIPLE。
 * - 文件大小上限 1MB，超出则拒绝导入。
 * - 仅接受白名单内的文本/代码扩展名，二进制文件一律拒绝。
 */
object SharedFileHandler {

    private const val TAG = "SharedFileHandler"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB

    /**
     * 支持的文本/代码文件扩展名白名单（含点号前缀，全小写）。
     * 覆盖主流编程语言、标记语言、配置文件与纯文本格式。
     */
    val SUPPORTED_EXTENSIONS: Set<String> = setOf(
        // Web 前端
        ".html", ".htm", ".js", ".jsx", ".ts", ".tsx", ".mjs",
        ".css", ".scss", ".less", ".vue", ".svelte",
        // 数据/配置
        ".json", ".xml", ".svg", ".yaml", ".yml", ".toml", ".ini", ".conf",
        // 文档
        ".md", ".markdown", ".txt", ".prompt", ".csv", ".log",
        // 编程语言
        ".py", ".pyw", ".java", ".kt", ".kts", ".c", ".h", ".cpp", ".hpp",
        ".cc", ".cs", ".go", ".rs", ".rb", ".php", ".swift", ".dart",
        ".r", ".lua", ".sql",
        // Shell/脚本
        ".sh", ".bash", ".zsh", ".bat"
    )

    /**
     * 解析系统分享文件 Intent，返回 [SharedFileResult]。
     *
     * @param context 应用 Context，用于获取 ContentResolver
     * @param intent  系统 ACTION_SEND Intent
     * @return [SharedFileResult.Success] 或 [SharedFileResult.Error]
     */
    fun parseSharedFile(context: Context, intent: Intent): SharedFileResult {
        // 1. 提取 content:// URI
        val uri: Uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            ?: return SharedFileResult.Error(R.string.share_file_read_error)

        // 2. 校验 MIME 类型（粗筛：排除 image/video/audio 等非文本类型）
        val mimeType = intent.type ?: context.contentResolver.getType(uri)
        if (!isTextMimeType(mimeType)) {
            return SharedFileResult.Error(R.string.share_file_unsupported)
        }

        // 3. 查询文件名
        val fileName = queryFileName(context, uri)

        // 4. 校验文件扩展名白名单
        val ext = getExtension(fileName)
        if (ext.isNotEmpty() && ext !in SUPPORTED_EXTENSIONS) {
            return SharedFileResult.Error(R.string.share_file_unsupported)
        }

        // 5. 查询文件大小并校验上限（查询失败返回 -1 时同样拒绝，防止 OOM）
        val fileSize = queryFileSize(context, uri)
        if (fileSize < 0 || fileSize > MAX_FILE_SIZE) {
            return SharedFileResult.Error(R.string.share_file_too_large)
        }

        // 6. 读取文件内容
        val content = try {
            readContent(context, uri)
        } catch (e: IOException) {
            return SharedFileResult.Error(R.string.share_file_read_error)
        } catch (e: SecurityException) {
            return SharedFileResult.Error(R.string.share_file_read_error)
        }

        // 7. 推断 SnippetType
        val detectedType = SnippetType.fromFileName(fileName)
        val sizeBytes = content.toByteArray(Charsets.UTF_8).size

        return SharedFileResult.Success(
            fileName = fileName,
            content = content,
            detectedType = detectedType,
            sizeBytes = sizeBytes
        )
    }

    /**
     * 判断 MIME 类型是否可能为文本/代码文件。
     *
     * 规则：
     * - text 类型一律放行
     * - application 类型中排除已知的二进制子类型
     * - image、video、audio 类型直接拒绝
     * - null 或未知类型放行（后续由扩展名白名单兜底）
     */
    fun isTextMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return true
        val lower = mimeType.lowercase()
        return when {
            lower.startsWith("text/") -> true
            lower.startsWith("image/") -> false
            lower.startsWith("video/") -> false
            lower.startsWith("audio/") -> false
            lower.startsWith("application/") -> {
                // 排除已知二进制 application 子类型
                val binarySubTypes = setOf(
                    "application/pdf",
                    "application/zip",
                    "application/gzip",
                    "application/octet-stream",
                    "application/x-rar-compressed",
                    "application/x-7z-compressed",
                    "application/x-tar",
                    "application/apk",
                    "application/vnd.android.package-archive"
                )
                lower !in binarySubTypes
            }
            else -> true // 未知类型放行，由扩展名白名单兜底
        }
    }

    /**
     * 通过 ContentResolver 查询文件的显示名称。
     * 若查询失败则从 URI 最后路径段提取，仍失败则返回默认名 "shared_file.txt"。
     */
    private fun queryFileName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
            // 查询失败，走降级逻辑
        }

        // 降级：从 URI 路径末段提取
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: ""
        }

        // 最终兜底
        if (name.isNullOrBlank()) {
            name = "shared_file.txt"
        }

        return name!!
    }

    /**
     * 通过 ContentResolver 查询文件字节大小。
     * 查询失败时返回 -1（后续读取时以实际内容长度为准）。
     */
    private fun queryFileSize(context: Context, uri: Uri): Long {
        var size: Long = -1
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            // 查询失败
        }
        return size
    }

    /**
     * 以 UTF-8 编码读取 content:// URI 的全部文本内容。
     * 自动检测并跳过 UTF-8 BOM (EF BB BF)。
     *
     * @throws IOException 当流读取失败时抛出
     */
    private fun readContent(context: Context, uri: Uri): String {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for uri: $uri")

        return inputStream.use { stream ->
            val bytes = stream.readBytes()
            // 检测并跳过 UTF-8 BOM (0xEF, 0xBB, 0xBF)
            val offset = if (bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) 3 else 0

            String(bytes, offset, bytes.size - offset, Charsets.UTF_8)
        }
    }

    /**
     * 提取文件名的小写扩展名（含点号），无扩展名时返回空字符串。
     */
    private fun getExtension(fileName: String): String {
        return if (fileName.contains('.')) {
            ".${fileName.substringAfterLast('.').lowercase()}"
        } else {
            ""
        }
    }
}
