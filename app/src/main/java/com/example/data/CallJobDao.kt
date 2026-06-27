package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallJobDao {
    @Query("SELECT * FROM call_jobs ORDER BY nextCallTime ASC")
    fun getAllJobsFlow(): Flow<List<CallJobEntity>>

    @Query("SELECT * FROM call_jobs WHERE isActive = 1 ORDER BY nextCallTime ASC")
    suspend fun getActiveJobs(): List<CallJobEntity>

    @Query("SELECT * FROM call_jobs WHERE id = :id")
    suspend fun getJobById(id: Long): CallJobEntity?

    @Insert
    suspend fun insert(job: CallJobEntity): Long

    @Update
    suspend fun update(job: CallJobEntity)

    @Query("DELETE FROM call_jobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_jobs")
    suspend fun clearJobs()
}
