package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shield.WebhookConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WebhookConfigDao {
    @Query("SELECT * FROM webhooks")
    fun getAllWebhooks(): Flow<List<WebhookConfig>>
    
    @Query("SELECT * FROM webhooks")
    suspend fun getAllWebhooksSync(): List<WebhookConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebhook(webhook: WebhookConfig)

    @Delete
    suspend fun deleteWebhook(webhook: WebhookConfig)
}
