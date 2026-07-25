package com.feige.snippetstudio.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * [SnippetDao] 数据访问对象 (Data Access Object) 接口。
 *
 * 教学解析：
 * 1. 响应式流 (Flow<List<T>>): 当 SQL 返回类型为 Flow 时，Room 会自动监听 `snippets` 表的变动。
 *    只要有任何 insert/update/delete 发生，Flow 就会自动从 SQLite 读取最新数据并主动 emit 给 UI。
 * 2. 协程挂起 (suspend): 对于单次执行的修改操作（如 upsert/delete），加上 `suspend` 关键字，
 *    确保该任务在协程中异步执行，不阻塞主 UI 线程。
 */
@Dao
interface SnippetDao {
    /**
     * 实时观察未放入回收站的所有活动代码片段，按最后修改时间降序 (`ORDER BY updatedAt DESC`) 排列。
     */
    @Query("SELECT * FROM snippets WHERE trashed=0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<SnippetEntity>>

    /**
     * 实时观察已星标收藏 (`starred=1`) 且未放入回收站的代码片段。
     */
    @Query("SELECT * FROM snippets WHERE trashed=0 AND starred=1 ORDER BY updatedAt DESC")
    fun observeStarred(): Flow<List<SnippetEntity>>

    /**
     * 实时观察回收站 (`trashed=1`) 中的代码片段，按移入回收站的时间倒序排列。
     */
    @Query("SELECT * FROM snippets WHERE trashed=1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<SnippetEntity>>

    /**
     * 根据类型代码 (如 "html", "js") 动态匹配过滤查询代码片段。
     * `:type` 参数会自动进行 SQL 参数防注入绑定。
     */
    @Query("SELECT * FROM snippets WHERE trashed=0 AND type=:type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<SnippetEntity>>

    /**
     * 根据 ID 异步获取单个代码片段实体对象（主键唯匹配，未查到返回 null）。
     */
    @Query("SELECT * FROM snippets WHERE id=:id")
    suspend fun byId(id: String): SnippetEntity?

    /**
     * 获取未被回收站删除的代码片段总数量。
     */
    @Query("SELECT COUNT(*) FROM snippets WHERE trashed=0")
    suspend fun activeCount(): Int

    /**
     * 原子操作：插入或更新实体 ([Upsert])。
     * 教学解析：Room 2.5+ 引入 `@Upsert`，在主键冲突时自动转化为 UPDATE，不存在时执行 INSERT，替代了旧版的 `@Insert(onConflict = OnConflictStrategy.REPLACE)`。
     */
    @Upsert
    suspend fun upsert(e: SnippetEntity)

    /**
     * 更新指定代码片段的收藏星标状态 (`starred` 标志位)。
     */
    @Query("UPDATE snippets SET starred=:s WHERE id=:id")
    suspend fun setStar(id: String, s: Boolean)

    /**
     * 软删除：将指定代码片段移入回收站 (标记 `trashed = 1` 并记录当前时间戳 `trashedAt`)。
     */
    @Query("UPDATE snippets SET trashed=1, trashedAt=:t WHERE id=:id")
    suspend fun trash(id: String, t: Long)

    /**
     * 还原软删除：从回收站恢复指定代码片段 (`trashed = 0` 并且重置 `trashedAt = NULL`)。
     */
    @Query("UPDATE snippets SET trashed=0, trashedAt=NULL WHERE id=:id")
    suspend fun restore(id: String)

    /**
     * 硬删除：从数据库中彻底物理清除指定 ID 的记录行 (不可恢复)。
     */
    @Query("DELETE FROM snippets WHERE id=:id")
    suspend fun purge(id: String)

    /**
     * 自动过期物理清理：清理在回收站中停放超过指定保留期限 (`trashedAt < :cutoff`) 的旧记录。
     */
    @Query("DELETE FROM snippets WHERE trashed=1 AND trashedAt < :cutoff")
    suspend fun purgeExpired(cutoff: Long)

    /**
     * 一次性获取当前所有活动代码片段的内存快照列表（适用于全局导出 ZIP / JSON，无需持续监听）。
     */
    @Query("SELECT * FROM snippets WHERE trashed=0")
    suspend fun allActiveSnapshot(): List<SnippetEntity>
}


