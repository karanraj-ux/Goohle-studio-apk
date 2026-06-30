package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val TARGET_NUMBERS = stringPreferencesKey("target_numbers")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val SENDERS = stringPreferencesKey("senders")
        val KEYWORD_FILTER = stringPreferencesKey("keyword_filter")
        val FORWARD_PHONE = stringPreferencesKey("forward_phone")
        val WEBHOOK_FILTER = stringPreferencesKey("webhook_filter")
        val ALLOW_EXTERNAL_AUTOMATION = booleanPreferencesKey("allow_external_automation")
        val AUTO_RESPOND_MISSED_CALL = booleanPreferencesKey("auto_respond_missed_call")
        val AUTO_RESPOND_SMS = booleanPreferencesKey("auto_respond_sms")
        val SILENT_SWALLOW = booleanPreferencesKey("silent_swallow")
        val MASTER_KILL_SWITCH = booleanPreferencesKey("master_kill_switch")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CALL_FORWARD_TARGET = stringPreferencesKey("call_forward_target")
        val VIP_DIVERT_NUMBER = stringPreferencesKey("vip_divert_number")
        
        val MERCHANT_KEYWORDS = stringPreferencesKey("merchant_keywords")
        val SCAM_KEYWORDS = stringPreferencesKey("scam_keywords")
        val VIP_CALLERS = stringPreferencesKey("vip_callers")
        val AUTO_FORWARD_CALLS = booleanPreferencesKey("auto_forward_calls")
        val AUTO_FORWARD_DURATION = intPreferencesKey("auto_forward_duration")
        val ALERT_FORWARD_TARGET = booleanPreferencesKey("alert_forward_target")
        val OVERRIDE_DND = booleanPreferencesKey("override_dnd")
        val DND_TIMEFRAME_MINUTES = intPreferencesKey("dnd_timeframe_minutes")
        val DND_THRESHOLD_CALLS = intPreferencesKey("dnd_threshold_calls")
        val DETECT_BUSY = booleanPreferencesKey("detect_busy")
        val BUSY_REPLY_MSG = stringPreferencesKey("busy_reply_msg")
        val SELECTED_SIM_ID = stringPreferencesKey("selected_sim_id")
        
        val SHOW_KJ_COMPANION = booleanPreferencesKey("show_kj_companion")
        val SHOW_KJ_AI = booleanPreferencesKey("show_kj_ai")
        val SHOW_CALLS = booleanPreferencesKey("show_calls")
        val SHOW_SHIELD = booleanPreferencesKey("show_shield")
        val SHOW_DECLUTTER = booleanPreferencesKey("show_declutter")
        
        val HAS_WELCOMED_KJ = booleanPreferencesKey("has_welcomed_kj")
        
        val WIDGET_RECENT_LOGS = booleanPreferencesKey("widget_recent_logs")
        val WIDGET_QUICK_CHAT = booleanPreferencesKey("widget_quick_chat")

        val AUTO_REPLY_ENABLED = booleanPreferencesKey("auto_reply_enabled")
        val BLOCK_SPAM_CALLS = booleanPreferencesKey("block_spam_calls")
        val SMS_FORWARDING_ENABLED = booleanPreferencesKey("sms_forwarding_enabled")
    }

    val targetNumbers: Flow<String> = context.dataStore.data.map { it[TARGET_NUMBERS] ?: "" }
    val webhookUrl: Flow<String> = context.dataStore.data.map { it[WEBHOOK_URL] ?: "" }
    val senders: Flow<String> = context.dataStore.data.map { it[SENDERS] ?: "" }
    val keywordFilter: Flow<String> = context.dataStore.data.map { it[KEYWORD_FILTER] ?: "" }
    val forwardPhone: Flow<String> = context.dataStore.data.map { it[FORWARD_PHONE] ?: "" }
    val webhookFilter: Flow<String> = context.dataStore.data.map { it[WEBHOOK_FILTER] ?: "ALL" }
    val allowExternalAutomation: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_EXTERNAL_AUTOMATION] ?: false }
    val autoRespondMissedCall: Flow<Boolean> = context.dataStore.data.map { it[AUTO_RESPOND_MISSED_CALL] ?: false }
    val autoRespondSms: Flow<Boolean> = context.dataStore.data.map { it[AUTO_RESPOND_SMS] ?: false }
    val silentSwallow: Flow<Boolean> = context.dataStore.data.map { it[SILENT_SWALLOW] ?: false }
    val masterKillSwitch: Flow<Boolean> = context.dataStore.data.map { it[MASTER_KILL_SWITCH] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
    val callForwardTarget: Flow<String> = context.dataStore.data.map { it[CALL_FORWARD_TARGET] ?: "" }
    val vipDivertNumber: Flow<String> = context.dataStore.data.map { it[VIP_DIVERT_NUMBER] ?: "" }
    
    val merchantKeywords: Flow<String> = context.dataStore.data.map { it[MERCHANT_KEYWORDS] ?: "" }
    val scamKeywords: Flow<String> = context.dataStore.data.map { it[SCAM_KEYWORDS] ?: "" }
    val vipCallers: Flow<String> = context.dataStore.data.map { it[VIP_CALLERS] ?: "" }
    val autoForwardCalls: Flow<Boolean> = context.dataStore.data.map { it[AUTO_FORWARD_CALLS] ?: false }
    val autoForwardDuration: Flow<Int> = context.dataStore.data.map { it[AUTO_FORWARD_DURATION] ?: 5 }
    val alertForwardTarget: Flow<Boolean> = context.dataStore.data.map { it[ALERT_FORWARD_TARGET] ?: false }
    val overrideDnd: Flow<Boolean> = context.dataStore.data.map { it[OVERRIDE_DND] ?: false }
    val dndTimeframeMinutes: Flow<Int> = context.dataStore.data.map { it[DND_TIMEFRAME_MINUTES] ?: 5 }
    val dndThresholdCalls: Flow<Int> = context.dataStore.data.map { it[DND_THRESHOLD_CALLS] ?: 2 }
    val detectBusy: Flow<Boolean> = context.dataStore.data.map { it[DETECT_BUSY] ?: false }
    val busyReplyMsg: Flow<String> = context.dataStore.data.map { it[BUSY_REPLY_MSG] ?: "I am currently in another call. I will call you back later." }
    val selectedSimId: Flow<String?> = context.dataStore.data.map { it[SELECTED_SIM_ID] }
    
    val showKjCompanion: Flow<Boolean> = context.dataStore.data.map { it[SHOW_KJ_COMPANION] ?: true }
    val showKjAi: Flow<Boolean> = context.dataStore.data.map { it[SHOW_KJ_AI] ?: true }
    val showCalls: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CALLS] ?: true }
    val showShield: Flow<Boolean> = context.dataStore.data.map { it[SHOW_SHIELD] ?: true }
    val showDeclutter: Flow<Boolean> = context.dataStore.data.map { it[SHOW_DECLUTTER] ?: true }
    
    val hasWelcomedKj: Flow<Boolean> = context.dataStore.data.map { it[HAS_WELCOMED_KJ] ?: false }
    
    val widgetRecentLogs: Flow<Boolean> = context.dataStore.data.map { it[WIDGET_RECENT_LOGS] ?: true }
    val widgetQuickChat: Flow<Boolean> = context.dataStore.data.map { it[WIDGET_QUICK_CHAT] ?: true }
    
    val autoReplyEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUTO_REPLY_ENABLED] ?: false }
    val blockSpamCalls: Flow<Boolean> = context.dataStore.data.map { it[BLOCK_SPAM_CALLS] ?: false }
    val smsForwardingEnabled: Flow<Boolean> = context.dataStore.data.map { it[SMS_FORWARDING_ENABLED] ?: false }

    private val cache = java.util.concurrent.ConcurrentHashMap<Preferences.Key<*>, Any>()

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            context.dataStore.data.collect { prefs ->
                prefs.asMap().forEach { (key, value) ->
                    cache[key] = value
                }
            }
        }
    }

    fun getStringSync(key: Preferences.Key<String>, default: String = ""): String {
        return (cache[key] as? String) ?: default
    }

    fun getIntSync(key: Preferences.Key<Int>, default: Int = 0): Int {
        return (cache[key] as? Int) ?: default
    }

    fun getBooleanSync(key: Preferences.Key<Boolean>, default: Boolean = false): Boolean {
        return (cache[key] as? Boolean) ?: default
    }

    suspend fun updateString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
    
    suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }
    
    suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
    
    suspend fun removeKey(key: Preferences.Key<String>) {
        context.dataStore.edit { it.remove(key) }
    }
}
