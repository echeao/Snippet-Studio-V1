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

object Exporter {

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

    fun createZipFile(context: Context, snippets: List<Snippet>): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(context.cacheDir, "snippet_studio_backup_$dateStr.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add manifest JSON
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(exportToJsonString(snippets).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Add individual files
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
