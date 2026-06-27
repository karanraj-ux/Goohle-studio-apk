package com.example.data.repository

import com.example.data.CallJobDao
import com.example.data.CallJobEntity
import kotlinx.coroutines.flow.Flow

class CallJobRepository(private val dbProvider: () -> com.example.data.AppDatabase) {
    private val dao get() = dbProvider().callJobDao()
    
    fun getAllJobsFlow(): Flow<List<CallJobEntity>> = dao.getAllJobsFlow()
    
    suspend fun getActiveJobs(): List<CallJobEntity> = dao.getActiveJobs()
    
    suspend fun getJobById(id: Long): CallJobEntity? = dao.getJobById(id)
    
    suspend fun insert(job: CallJobEntity): Long = dao.insert(job)
    
    suspend fun update(job: CallJobEntity) = dao.update(job)
    
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
