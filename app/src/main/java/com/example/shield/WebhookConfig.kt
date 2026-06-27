package com.example.shield

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "webhooks")
data class WebhookConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val method: String = "POST", // GET, POST, PUT
    val headersJson: String = "{}", // JSON object of headers
    val customPayload: String = "" // if empty, use default JSON
)
