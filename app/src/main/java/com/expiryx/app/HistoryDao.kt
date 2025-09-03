package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History)

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<History>>

    // ✅ move this here (was wrongly in ProductDao)
    @Query("SELECT * FROM history_table WHERE productId = :productId AND action = :action LIMIT 1")
    suspend fun findByProductAndAction(productId: Int, action: String): History?
}
