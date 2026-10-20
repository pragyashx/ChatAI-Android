package com.chatai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatai.app.data.local.entity.CodeAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeAttemptDao {
    @Query("SELECT * FROM code_attempts ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentAttempts(limit: Int): Flow<List<CodeAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: CodeAttemptEntity)
}
