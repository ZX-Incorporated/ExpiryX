package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History)

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<History>>

    @Query("SELECT * FROM history_table WHERE productId = :productId AND action = :action LIMIT 1")
    suspend fun findByProductAndAction(productId: Int, action: String): History?

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    suspend fun getAllHistoryNow(): List<History>

    @Query("DELETE FROM history_table")
    suspend fun clearAllHistory()

    @Query("DELETE FROM history_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}