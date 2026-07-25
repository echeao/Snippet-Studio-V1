package com.feige.snippetstudio.util

import android.content.Context
import android.net.Uri
import com.feige.snippetstudio.model.Snippet
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * [Exporter] 是数据导出与备份工具单例对象。
 *
 * 支持：
 * 1. 导出整套 Snippet 数据列表为标准格式化的 JSON 字符串 / JSON 文件。
 * 2. 将数据全量打包封装为含有 `manifest.json` 和子源码目录的 `.zip` 压缩归档文件，方便用户备份与分享。
 */
object Exporter {

    /**
     * 将给定的代码片段列表序列化转换为易读的 JSON 格式化字符串。
     *
     * @param snippets 代码片段模型列表
     * @return 格式化后的 JSON 字符串 (缩进 2 个空格)
     */
    fun exportToJsonString(snippets: List<Snippet>): String {
        val array = JSONArray()
        for (snippet in snippets) {
            val obj = JSONObject().apply {
                put("id", snippet.id)
                put("type", snippet.type.code)
                put("title", snippet.title)
                put("fileName", snippet.fileName)
                put("content", snippet.content)
                put("tags", JSONArray(snippet.tags))
                put("starred", snippet.starred)
                put("createdAt", snippet.createdAt)
                put("updatedAt", snippet.updatedAt)
                put("sizeBytes", snippet.sizeBytes)
            }
            array.put(obj)
        }
        val root = JSONObject().apply {
            put("app", "Snippet Studio")
            put("version", "1.0.0")
            put("exportedAt", System.currentTimeMillis())
            put("count", snippets.size)
            put("snippets", array)
        }
        return root.toString(2)
    }

    /**
     * 将代码片段 JSON 数据写入系统 SAF 提供的目标 [Uri] 输出流中。
     *
     * @param context 上下文
     * @param snippets 代码片段列表
     * @param uri 写入目标的 Uri 对象
     * @return 是否写入成功
     */
    fun exportToJsonFile(context: Context, snippets: List<Snippet>, uri: Uri): Boolean {
        return try {
            val jsonStr = exportToJsonString(snippets)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(jsonStr)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建全量代码片段的 ZIP 压缩备份包文件。
     *
     * ZIP 结构说明：
     * - `manifest.json`: 记录全量片段的元数据描述清单
     * - `snippets/`: 存放每个片段独立的原始文本/源码物理文件
     *
     * @param context 上下文对象
     * @param snippets 需要打包的代码片段列表
     * @return 生成在缓存目录中的 [File] 实例，若失败则返回 null
     */
    /**
     * 创建全量代码片段的 ZIP 压缩备份包文件。
     *
     * 教学解析：
     * 1. `ZipOutputStream`: Java 标准库提供的压缩输出流对象，用于构建多文件归档包。
     * 2. `ZipEntry`: 代表 ZIP 文件内部的一个节点实体（可以是文件或相对目录路径）。
     * 3. 流式写入与关闭: 每次写入一个 ZipEntry 时，先调用 `putNextEntry()` 宣告开始，
     *    写入完成调用 `closeEntry()` 刷新缓冲，最后由 `.use` 闭包自动释放 `ZipOutputStream` 句柄。
     *
     * @param context 上下文对象
     * @param snippets 需要打包的代码片段列表
     * @return 生成在缓存目录 cacheDir 中的 [File] 实例，若失败则返回 null
     */
    fun createZipFile(context: Context, snippets: List<Snippet>): File? {
        return try {
            // 使用 SimpleDateFormat 生成无空格安全文件名 (例如 snippet_studio_backup_20260725_103000.zip)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(context.cacheDir, "snippet_studio_backup_$dateStr.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // ===== 步骤 1: 写入 manifest.json 根元数据描述清单 =====
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(exportToJsonString(snippets).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // ===== 步骤 2: 将每个代码片段单独落盘写入 snippets/ 文件夹归档 =====
                for (snippet in snippets) {
                    val filename = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
                    val entryName = "snippets/$filename"
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    zos.write(snippet.content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


