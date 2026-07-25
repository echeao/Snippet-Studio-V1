package com.feige.snippetstudio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * [AppDatabase] 是应用采用的 Room 抽象数据库基类。
 *
 * 架构原理：
 * 1. Room 是 Android 官方在 SQLite 之上的 ORM 封装库。它在编译期利用 KSP/APT 校验 SQL 语法并生成真实 DAO 实现类。
 * 2. 数据库升版机制：当 AppEntity 表结构增加/改动字段时，需要提升 version 版本号，并提供 Migration 迁移对象，
 *    否则数据库在老用户升级 App 后首次打开时会因为 Schema 不匹配而崩溃。
 *
 * @property entities 数据库包含的表实体数组 [SnippetEntity], [FolderEntity]
 * @property version 数据库当前版本号 (版本 3 新增了 folders 文件夹实体表)
 * @property exportSchema 是否导出 Schema 架构 JSON 描述文件（生产环境下设为 false 减少构建产物体积）
 */
@Database(entities = [SnippetEntity::class, FolderEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 抽象方法：获取 [SnippetDao] 数据访问对象接口。
     * Room 在编译期会自动生成该抽象方法的具体派生实现。
     */
    abstract fun snippetDao(): SnippetDao

    /**
     * 抽象方法：获取 [FolderDao] 文件夹持久化数据访问对象接口。
     */
    abstract fun folderDao(): FolderDao

    companion object {
        /**
         * 数据库版本迁移脚本：从版本 1 (v1) 平滑升级至版本 2 (v2)。
         * 
         * 教学解析：
         * 使用 SQL `ALTER TABLE` 语句增量添加列 `folder`，类型为 TEXT，非空且默认值为空字符串 `''`。
         * 这样能保证老用户旧版本数据完好无损，且无缝具备新的文件夹层级管理功能。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snippets ADD COLUMN folder TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * 数据库版本迁移脚本：从版本 2 (v2) 平滑升级至版本 3 (v3)。
         *
         * 教学解析：
         * 使用 SQL `CREATE TABLE` 语句创建新的 `folders` 实体表。
         * 存储文件夹相对路径 `path` (主键)、父级路径 `parentPath` 以及创建时间戳 `createdAt`。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        path TEXT PRIMARY KEY NOT NULL,
                        parentPath TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * 构建并创建 [AppDatabase] 实例。
         *
         * @param ctx 上下文对象 (内部自动转为 applicationContext 防止泄露 Activity)
         * @return 编译完成的 RoomDatabase 数据库操作句柄
         */
        fun create(ctx: Context): AppDatabase = Room.databaseBuilder(
            ctx.applicationContext,
            AppDatabase::class.java,
            "snippet_studio.db" // 物理存储在 app 沙盒 databases/snippet_studio.db 中
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // 优先尝试执行 SQL 平滑迁移
            .fallbackToDestructiveMigration() // 兜底策略：当版本跨度过大或无对应 Migration 脚本时重建表，防止 Fatal Crash
            .build()
    }
}


