package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val assistantName: String = "Assistant",
    val assistantAvatar: String = "",
    val targetNumbers: String = "",
    val webhookUrl: String = "",
    val forwardPhone: String = "",
    val webhookFilter: String = "SMS,CALL",
    val allowExternalAutomation: Boolean = true,
    val autoRespondMissedCall: Boolean = false,
    val autoReplyRestrictedNumbers: String = "",
    val autoRespondSms: Boolean = false,
    val silentSwallow: Boolean = true,
    val masterKillSwitch: Boolean = false,
    val appTheme: String = "system",
    val senders: String = "",
    val keywordFilter: String = "",
    val merchantKeywords: String = "bank,alert,txn,otp,code",
    val vipCallers: String = "",
    val autoForwardCalls: Boolean = false,
    val callForwardTarget: String = "",
    val autoForwardDuration: String = "5",
    val alertForwardTarget: Boolean = false,
    val vipDivertNumber: String = "",
    val overrideDnd: Boolean = false,
    val dndTimeframeMinutes: String = "5",
    val dndThresholdCalls: String = "2",
    val hasSeenShieldTooltip: Boolean = false,
    val detectBusyAndReply: Boolean = false,
    val busyReplyMessage: String = "I'm currently busy. Please leave a message.",
    val selectedSimId: String? = null,
    val showKjCompanion: Boolean = true,
    val showCalls: Boolean = true,
    val showShield: Boolean = true,
    val widgetRecentLogs: Boolean = true,
    val widgetQuickChat: Boolean = true,
    val blockSpamCalls: Boolean = true,
    val ghostMode: Boolean = false,
    val ghostModePauseEndTime: Long = 0L,
    val smartSpamReader: Boolean = false,
    val smsForwardingEnabled: Boolean = false,
    val smsForwardTarget: String = "",
    val dndBypassRingtoneUri: String = "",
    val guardianName: String = "",
    val guardianNumber: String = "",
    val extractOtps: Boolean = false,
    val forwardServiceSmsOnly: Boolean = false,
    val spamBlockedCount: Int = 0
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.spamBlockedCount.collect { count ->
                _uiState.update { it.copy(spamBlockedCount = count) }
            }
        }
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    spamBlockedCount = settingsRepository.spamBlockedCount.first(),
                    assistantName = settingsRepository.assistantName.first(),
                    assistantAvatar = settingsRepository.assistantAvatar.first(),
                    targetNumbers = settingsRepository.targetNumbers.first(),
                    webhookUrl = settingsRepository.webhookUrl.first(),
                    forwardPhone = settingsRepository.forwardPhone.first(),
                    webhookFilter = settingsRepository.webhookFilter.first(),
                    allowExternalAutomation = settingsRepository.allowExternalAutomation.first(),
                    autoRespondMissedCall = settingsRepository.autoRespondMissedCall.first(),
                    autoReplyRestrictedNumbers = settingsRepository.autoReplyRestrictedNumbers.first(),
                    autoRespondSms = settingsRepository.autoRespondSms.first(),
                    silentSwallow = settingsRepository.silentSwallow.first(),
                    masterKillSwitch = settingsRepository.masterKillSwitch.first(),
                    ghostModePauseEndTime = settingsRepository.getLongSync(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, 0L),
                    senders = settingsRepository.senders.first(),
                    keywordFilter = settingsRepository.keywordFilter.first(),
                    merchantKeywords = settingsRepository.merchantKeywords.first(),
                    vipCallers = settingsRepository.vipCallers.first(),
                    autoForwardCalls = settingsRepository.autoForwardCalls.first(),
                    callForwardTarget = settingsRepository.callForwardTarget.first(),
                    autoForwardDuration = settingsRepository.autoForwardDuration.first().toString(),
                    alertForwardTarget = settingsRepository.alertForwardTarget.first(),
                    vipDivertNumber = settingsRepository.vipDivertNumber.first(),
                    overrideDnd = settingsRepository.overrideDnd.first(),
                    hasSeenShieldTooltip = settingsRepository.hasSeenShieldTooltip.first(),
                    dndTimeframeMinutes = settingsRepository.dndTimeframeMinutes.first().toString(),
                    dndThresholdCalls = settingsRepository.dndThresholdCalls.first().toString(),
                    detectBusyAndReply = settingsRepository.detectBusy.first(),
                    busyReplyMessage = settingsRepository.busyReplyMsg.first(),
                    selectedSimId = settingsRepository.selectedSimId.first(),
                    showKjCompanion = settingsRepository.showKjCompanion.first(),
                    showCalls = settingsRepository.showCalls.first(),
                    showShield = settingsRepository.showShield.first(),
                    widgetRecentLogs = settingsRepository.widgetRecentLogs.first(),
                    widgetQuickChat = settingsRepository.widgetQuickChat.first(),
                    blockSpamCalls = settingsRepository.blockSpamCalls.first(),
                    ghostMode = settingsRepository.ghostMode.first(),
                    smartSpamReader = settingsRepository.smartSpamReader.first(),
                    smsForwardingEnabled = settingsRepository.smsForwardingEnabled.first(),
                    smsForwardTarget = settingsRepository.smsForwardTarget.first(),
                    dndBypassRingtoneUri = settingsRepository.dndBypassRingtoneUri.first(),
                    guardianName = settingsRepository.getStringSync(androidx.datastore.preferences.core.stringPreferencesKey("guardian_name"), ""),
                    guardianNumber = settingsRepository.getStringSync(androidx.datastore.preferences.core.stringPreferencesKey("guardian_number"), ""),
                    extractOtps = settingsRepository.extractOtps.first()
                )
            }
        }
    }
    
    fun updateAssistantName(value: String) { _uiState.update { it.copy(assistantName = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.ASSISTANT_NAME, value) } }
    fun updateAssistantAvatar(value: String) { _uiState.update { it.copy(assistantAvatar = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.ASSISTANT_AVATAR, value) } }
    fun updateTargetNumbers(value: String) { _uiState.update { it.copy(targetNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) } }
    fun updateWebhookUrl(value: String) { _uiState.update { it.copy(webhookUrl = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.WEBHOOK_URL, value) } }
    fun updateForwardPhone(value: String) { _uiState.update { it.copy(forwardPhone = value, targetNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.FORWARD_PHONE, value); settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) } }
    fun updateWebhookFilter(value: String) { _uiState.update { it.copy(webhookFilter = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.WEBHOOK_FILTER, value) } }
    fun updateAllowExternalAutomation(value: Boolean) { _uiState.update { it.copy(allowExternalAutomation = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALLOW_EXTERNAL_AUTOMATION, value) } }
    fun updateAutoRespondMissedCall(value: Boolean) { _uiState.update { it.copy(autoRespondMissedCall = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_MISSED_CALL, value) } }
    fun updateAutoReplyRestrictedNumbers(value: String) { _uiState.update { it.copy(autoReplyRestrictedNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.AUTO_REPLY_RESTRICTED_NUMBERS, value) } }
    fun updateAutoRespondSms(value: Boolean) { _uiState.update { it.copy(autoRespondSms = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_SMS, value) } }
    fun updateSilentSwallow(value: Boolean) { _uiState.update { it.copy(silentSwallow = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SILENT_SWALLOW, value) } }
    fun updateMasterKillSwitch(value: Boolean) { _uiState.update { it.copy(masterKillSwitch = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.MASTER_KILL_SWITCH, value) } }
    fun updateSenders(value: String) { _uiState.update { it.copy(senders = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SENDERS, value) } }
    fun updateKeywordFilter(value: String) { _uiState.update { it.copy(keywordFilter = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.KEYWORD_FILTER, value) } }
    fun updateMerchantKeywords(value: String) { _uiState.update { it.copy(merchantKeywords = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.MERCHANT_KEYWORDS, value) } }
    fun updateVipCallers(value: String) { _uiState.update { it.copy(vipCallers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_CALLERS, value) } }
    fun updateAutoForwardCalls(value: Boolean) { _uiState.update { it.copy(autoForwardCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_FORWARD_CALLS, value) } }
    fun updateCallForwardTarget(value: String) { _uiState.update { it.copy(callForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.CALL_FORWARD_TARGET, value) } }
    fun updateAutoForwardDuration(value: String) { _uiState.update { it.copy(autoForwardDuration = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.AUTO_FORWARD_DURATION, value.toIntOrNull() ?: 5) } }
    fun updateAlertForwardTarget(value: Boolean) { _uiState.update { it.copy(alertForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALERT_FORWARD_TARGET, value) } }
    fun updateVipDivertNumber(value: String) { _uiState.update { it.copy(vipDivertNumber = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_DIVERT_NUMBER, value) } }
    fun updateOverrideDnd(value: Boolean) { _uiState.update { it.copy(overrideDnd = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.OVERRIDE_DND, value) } }
    fun updateHasSeenShieldTooltip(value: Boolean) { _uiState.update { it.copy(hasSeenShieldTooltip = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.HAS_SEEN_SHIELD_TOOLTIP, value) } }
    fun updateDndTimeframeMinutes(value: String) { _uiState.update { it.copy(dndTimeframeMinutes = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_TIMEFRAME_MINUTES, value.toIntOrNull() ?: 5) } }
    fun updateDndThresholdCalls(value: String) { _uiState.update { it.copy(dndThresholdCalls = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_THRESHOLD_CALLS, value.toIntOrNull() ?: 2) } }
    fun updateDetectBusyAndReply(value: Boolean) { _uiState.update { it.copy(detectBusyAndReply = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.DETECT_BUSY, value) } }
    fun updateBusyReplyMessage(value: String) { _uiState.update { it.copy(busyReplyMessage = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.BUSY_REPLY_MSG, value) } }
    fun updateSelectedSimId(value: String?) { _uiState.update { it.copy(selectedSimId = value) }; viewModelScope.launch { if (value == null) settingsRepository.removeKey(SettingsRepository.SELECTED_SIM_ID) else settingsRepository.updateString(SettingsRepository.SELECTED_SIM_ID, value) } }
    fun updateShowKjCompanion(value: Boolean) { _uiState.update { it.copy(showKjCompanion = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_KJ_COMPANION, value) } }
    fun updateShowCalls(value: Boolean) { _uiState.update { it.copy(showCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_CALLS, value) } }
    fun updateShowShield(value: Boolean) { _uiState.update { it.copy(showShield = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_SHIELD, value) } }
    fun updateWidgetRecentLogs(value: Boolean) { _uiState.update { it.copy(widgetRecentLogs = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_RECENT_LOGS, value) } }
    fun updateWidgetQuickChat(value: Boolean) { _uiState.update { it.copy(widgetQuickChat = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_QUICK_CHAT, value) } }
    fun updateBlockSpamCalls(value: Boolean) { _uiState.update { it.copy(blockSpamCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.BLOCK_SPAM_CALLS, value) } }
    fun updateGhostMode(value: Boolean) { _uiState.update { it.copy(ghostMode = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.GHOST_MODE, value) } }
    fun pauseGhostMode(durationMs: Long = 60 * 60 * 1000L) {
        val pauseEndTime = System.currentTimeMillis() + durationMs
        _uiState.update { it.copy(ghostModePauseEndTime = pauseEndTime) }
        viewModelScope.launch { settingsRepository.updateLong(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, pauseEndTime) }
    }
    fun updateSmartSpamReader(value: Boolean) { _uiState.update { it.copy(smartSpamReader = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SMART_SPAM_READER, value) } }
    fun updateSmsForwardingEnabled(value: Boolean) { _uiState.update { it.copy(smsForwardingEnabled = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SMS_FORWARDING_ENABLED, value) } }
    fun updateSmsForwardTarget(value: String) { _uiState.update { it.copy(smsForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SMS_FORWARD_TARGET, value) } }
    fun updateExtractOtps(value: Boolean) { _uiState.update { it.copy(extractOtps = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.EXTRACT_OTPS, value) } }
    fun updateAppTheme(value: String) { _uiState.update { it.copy(appTheme = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.APP_THEME, value) } }
    fun updateForwardServiceSmsOnly(value: Boolean) { _uiState.update { it.copy(forwardServiceSmsOnly = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.FORWARD_SERVICE_SMS_ONLY, value) } }
    fun updateDndBypassRingtoneUri(uri: String) { _uiState.update { it.copy(dndBypassRingtoneUri = uri) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.DND_BYPASS_RINGTONE_URI, uri) } }
    fun updateGuardianName(value: String) { _uiState.update { it.copy(guardianName = value) }; viewModelScope.launch { settingsRepository.updateString(androidx.datastore.preferences.core.stringPreferencesKey("guardian_name"), value) } }
    fun updateGuardianNumber(value: String) { _uiState.update { it.copy(guardianNumber = value) }; viewModelScope.launch { settingsRepository.updateString(androidx.datastore.preferences.core.stringPreferencesKey("guardian_number"), value) } }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
