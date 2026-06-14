package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_jobs")
data class CallJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val totalCalls: Int,
    val callsMade: Int = 0,
    val intervalMinutes: Int,
    val nextCallTime: Long,
    val isActive: Boolean = true,
    val description: String = ""
)
