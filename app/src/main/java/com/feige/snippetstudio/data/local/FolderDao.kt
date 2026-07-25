package com.feige.snippetstudio.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * [FolderDao] 文件夹持久化表的数据访问接口 (Data Access Object)。
 *
 * 核心职责：
 * 1. 响应式监听 SQLite 数据库中的 `folders` 表，当有文件夹新建或删除时实时 notify UI 树状视图。
 * 2. 提供单次插入/替换（Upsert）与删除（Delete）原子操作。
 */
@Dao
interface FolderDao {

    /**
     * 实时观察所有文件夹路径列表，按相对路径字典序升序 (`ORDER BY path ASC`) 排列。
     *
     * @return 包含全量 [FolderEntity] 的响应式 Flow 流
     */
    @Query("SELECT * FROM folders ORDER BY path ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    /**
     * 原子操作：新增或更新文件夹实体 ([Upsert])。
     * 若主键 [FolderEntity.path] 冲突则进行更新，否则新增。
     *
     * @param folder 待保存的文件夹实体对象
     */
    @Upsert
    suspend fun upsert(folder: FolderEntity)

    /**
     * 批量插入或更新文件夹实体对象列表。
     *
     * @param folders 文件夹实体列表
     */
    @Upsert
    suspend fun upsertAll(folders: List<FolderEntity>)

    /**
     * 根据相对路径硬删除指定的文件夹记录。
     *
     * @param path 待删除的文件夹相对路径
     */
    @Query("DELETE FROM folders WHERE path = :path")
    suspend fun deleteByPath(path: String)

    /**
     * 获取当前数据库中全量文件夹实体的内存快照列表（非响应式单次查询）。
     *
     * @return 全量 [FolderEntity] 快照列表
     */
    @Query("SELECT * FROM folders")
    suspend fun allSnapshot(): List<FolderEntity>
}
