package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility module that provides read-only SQL query access to the local SQLite database.
 * Specifically designed to aggregate and summarize SMS, OTP, and financial records 
 * for the KJ AI interface and for local data export.
 */
class KjDatabaseUtility(private val context: Context) {
    private val db by lazy { (context.applicationContext as com.example.ShieldApplication).container.database }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }
    
    suspend fun getAggregateLogsForAI(): String = withContext(Dispatchers.IO) {
        val smsList = db.smsLogDao().getRecentLogs().first()
        val expenseList = db.expenseDao().getAllExpenses().first()
        val callList = db.callJobDao().getAllJobsFlow().first()

        val json = JSONObject()

        val smsArr = JSONArray()
        smsList.take(100).forEach { 
            val obj = JSONObject()
            obj.put("sender", it.sender)
            obj.put("message", it.message)
            obj.put("time", it.timestamp)
            smsArr.put(obj)
        }
        json.put("recent_sms_otps", smsArr)

        val expArr = JSONArray()
        expenseList.take(100).forEach {
            val obj = JSONObject()
            obj.put("merchant", it.merchant)
            obj.put("amount", it.amountStr)
            expArr.put(obj)
        }
        json.put("recent_expenses", expArr)

        val callsArr = JSONArray()
        callList.forEach {
            val obj = JSONObject()
            obj.put("desc", it.description)
            obj.put("nextTime", it.nextCallTime)
            callsArr.put(obj)
        }
        json.put("planned_calls", callsArr)

        json.toString(2)
    }
    
    suspend fun exportAllDataAsJson(): String = withContext(Dispatchers.IO) {
        // Expand to include all records
        val smsList = db.smsLogDao().getRecentLogs().first()
        val expenseList = db.expenseDao().getAllExpenses().first()
        val callList = db.callJobDao().getAllJobsFlow().first()
        
        val json = JSONObject()

        val smsArr = JSONArray()
        smsList.forEach { 
            val obj = JSONObject()
            obj.put("sender", it.sender)
            obj.put("message", it.message)
            obj.put("time", it.timestamp)
            smsArr.put(obj)
        }
        json.put("all_sms_logs", smsArr)

        val expArr = JSONArray()
        expenseList.forEach {
            val obj = JSONObject()
            obj.put("merchant", it.merchant)
            obj.put("amount", it.amountStr)
            expArr.put(obj)
        }
        json.put("all_expenses", expArr)

        val callsArr = JSONArray()
        callList.forEach {
            val obj = JSONObject()
            obj.put("desc", it.description)
            obj.put("nextTime", it.nextCallTime)
            callsArr.put(obj)
        }
        json.put("all_planned_calls", callsArr)

        json.toString(2)
    }
}
