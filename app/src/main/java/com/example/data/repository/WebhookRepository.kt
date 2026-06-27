package com.example.data.repository

import com.example.shield.WebhookConfig
import com.example.db.WebhookConfigDao
import kotlinx.coroutines.flow.Flow

class WebhookRepository(private val webhookDao: WebhookConfigDao) {
    val allWebhooks: Flow<List<WebhookConfig>> = webhookDao.getAllWebhooks()

    suspend fun getAllWebhooksSync(): List<WebhookConfig> = webhookDao.getAllWebhooksSync()

    suspend fun insertWebhook(webhook: WebhookConfig) {
        webhookDao.insertWebhook(webhook)
    }

    suspend fun deleteWebhook(webhook: WebhookConfig) {
        webhookDao.deleteWebhook(webhook)
    }
}
