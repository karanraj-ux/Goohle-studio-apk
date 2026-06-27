package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.repository.CallJobRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FinancialRepository
import com.example.data.repository.SmsRepository
import com.example.data.repository.RuleRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.WebhookRepository

interface AppContainer {
    val database: AppDatabase
    val smsRepository: SmsRepository
    val callJobRepository: CallJobRepository
    val financialRepository: FinancialRepository
    val chatRepository: ChatRepository
    val ruleRepository: RuleRepository
    val webhookRepository: WebhookRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }
    
    override val smsRepository: SmsRepository by lazy {
        SmsRepository { database }
    }
    
    override val callJobRepository: CallJobRepository by lazy {
        CallJobRepository { database }
    }
    
    override val financialRepository: FinancialRepository by lazy {
        FinancialRepository( { database } )
    }
    
    override val chatRepository: ChatRepository by lazy {
        ChatRepository { database }
    }
    
    override val ruleRepository: RuleRepository by lazy {
        RuleRepository(database.customRuleDao())
    }
    
    override val webhookRepository: WebhookRepository by lazy {
        WebhookRepository(database.webhookConfigDao())
    }
    
    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }
}
