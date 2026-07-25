package com.feige.snippetstudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [FolderEntity] 是 Room 持久化数据库表 `folders` 的映射实体数据类 (Database Entity)。
 *
 * 架构原理：
 * 将文件夹路径升维为数据库中独立存在的元数据实体。
 * 解决物理系统创建的空文件夹在数据库中无记录、以及应用内无法提前创建空文件夹的问题。
 *
 * @param path 主键，文件夹相对路径（如 "components" 或 "utils/string"，留空字符串表示根目录无对应实体）
 * @param parentPath 父级文件夹相对路径（如 "components" 的父路径为 ""，"utils/string" 的父路径为 "utils"）
 * @param createdAt 文件夹创建时间戳（毫秒）
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val path: String,
    val parentPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
