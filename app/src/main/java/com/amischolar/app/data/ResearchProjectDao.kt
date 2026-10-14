package com.amischolar.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResearchProjectDao {
    @Query("SELECT * FROM research_projects ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ResearchProject>>

    @Query("SELECT * FROM research_projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ResearchProject?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ResearchProject): Long

    @Query("DELETE FROM research_projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
