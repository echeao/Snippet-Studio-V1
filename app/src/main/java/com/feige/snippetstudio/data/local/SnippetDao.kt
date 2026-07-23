package com.feige.snippetstudio.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets WHERE trashed=0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE trashed=0 AND starred=1 ORDER BY updatedAt DESC")
    fun observeStarred(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE trashed=1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE trashed=0 AND type=:type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE id=:id")
    suspend fun byId(id: String): SnippetEntity?

    @Query("SELECT COUNT(*) FROM snippets WHERE trashed=0")
    suspend fun activeCount(): Int

    @Upsert
    suspend fun upsert(e: SnippetEntity)

    @Query("UPDATE snippets SET starred=:s WHERE id=:id")
    suspend fun setStar(id: String, s: Boolean)

    @Query("UPDATE snippets SET trashed=1, trashedAt=:t WHERE id=:id")
    suspend fun trash(id: String, t: Long)

    @Query("UPDATE snippets SET trashed=0, trashedAt=NULL WHERE id=:id")
    suspend fun restore(id: String)

    @Query("DELETE FROM snippets WHERE id=:id")
    suspend fun purge(id: String)

    @Query("DELETE FROM snippets WHERE trashed=1 AND trashedAt < :cutoff")
    suspend fun purgeExpired(cutoff: Long)

    @Query("SELECT * FROM snippets WHERE trashed=0")
    suspend fun allActiveSnapshot(): List<SnippetEntity>
}
