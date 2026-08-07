package com.example.data.repository

import com.example.data.PhoneRuleEntity
import kotlinx.coroutines.flow.Flow

class PhoneRuleRepository(private val dbProvider: () -> com.example.data.AppDatabase) {
    private val dao get() = dbProvider().phoneRuleDao()
    
    fun getAllRules(): Flow<List<PhoneRuleEntity>> = dao.getAllRules()
    suspend fun getAllRulesSync(): List<PhoneRuleEntity> = dao.getAllRulesSync()
    
    suspend fun getRuleByNumber(number: String): PhoneRuleEntity? = dao.getRuleByNumber(number)
    
    suspend fun insert(rule: PhoneRuleEntity) = dao.insert(rule)
    
    suspend fun delete(rule: PhoneRuleEntity) = dao.delete(rule)
}
