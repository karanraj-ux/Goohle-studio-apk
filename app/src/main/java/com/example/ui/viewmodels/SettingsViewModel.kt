package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val targetNumbers: String = "",
    val webhookUrl: String = "",
    val forwardPhone: String = "",
    val webhookFilter: String = "ALL",
    val allowExternalAutomation: Boolean = false,
    val autoRespondMissedCall: Boolean = false,
    val autoRespondSms: Boolean = false,
    val silentSwallow: Boolean = false,
    val masterKillSwitch: Boolean = false,
    val senders: String = "",
    val keywordFilter: String = "",
    val merchantKeywords: String = "",
    val scamKeywords: String = "",
    val vipCallers: String = "",
    val autoForwardCalls: Boolean = false,
    val callForwardTarget: String = "",
    val autoForwardDuration: String = "5",
    val alertForwardTarget: Boolean = false,
    val vipDivertNumber: String = "",
    val overrideDnd: Boolean = false,
    val dndTimeframeMinutes: String = "5",
    val dndThresholdCalls: String = "2",
    val detectBusyAndReply: Boolean = false,
    val busyReplyMessage: String = "I am currently in another call. I will call you back later.",
    val selectedSimId: String? = null,
    val showKjCompanion: Boolean = true,
    val showKjAi: Boolean = true,
    val showCalls: Boolean = true,
    val showShield: Boolean = true,
    val showDeclutter: Boolean = true,
    val widgetRecentLogs: Boolean = true,
    val widgetQuickChat: Boolean = true,
    val smsForwardingEnabled: Boolean = false
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(
                targetNumbers = settingsRepository.targetNumbers.first(),
                webhookUrl = settingsRepository.webhookUrl.first(),
                forwardPhone = settingsRepository.forwardPhone.first(),
                webhookFilter = settingsRepository.webhookFilter.first(),
                allowExternalAutomation = settingsRepository.allowExternalAutomation.first(),
                autoRespondMissedCall = settingsRepository.autoRespondMissedCall.first(),
                autoRespondSms = settingsRepository.autoRespondSms.first(),
                silentSwallow = settingsRepository.silentSwallow.first(),
                masterKillSwitch = settingsRepository.masterKillSwitch.first(),
                senders = settingsRepository.senders.first(),
                keywordFilter = settingsRepository.keywordFilter.first(),
                merchantKeywords = settingsRepository.merchantKeywords.first(),
                scamKeywords = settingsRepository.scamKeywords.first(),
                vipCallers = settingsRepository.vipCallers.first(),
                autoForwardCalls = settingsRepository.autoForwardCalls.first(),
                callForwardTarget = settingsRepository.callForwardTarget.first(),
                autoForwardDuration = settingsRepository.autoForwardDuration.first().toString(),
                alertForwardTarget = settingsRepository.alertForwardTarget.first(),
                vipDivertNumber = settingsRepository.vipDivertNumber.first(),
                overrideDnd = settingsRepository.overrideDnd.first(),
                dndTimeframeMinutes = settingsRepository.dndTimeframeMinutes.first().toString(),
                dndThresholdCalls = settingsRepository.dndThresholdCalls.first().toString(),
                detectBusyAndReply = settingsRepository.detectBusy.first(),
                busyReplyMessage = settingsRepository.busyReplyMsg.first(),
                selectedSimId = settingsRepository.selectedSimId.first(),
                showKjCompanion = settingsRepository.showKjCompanion.first(),
                showKjAi = settingsRepository.showKjAi.first(),
                showCalls = settingsRepository.showCalls.first(),
                showShield = settingsRepository.showShield.first(),
                showDeclutter = settingsRepository.showDeclutter.first(),
                widgetRecentLogs = settingsRepository.widgetRecentLogs.first(),
                widgetQuickChat = settingsRepository.widgetQuickChat.first(),
                smsForwardingEnabled = settingsRepository.smsForwardingEnabled.first()
            ) }
        }
    }

    fun updateTargetNumbers(value: String) {
        _uiState.update { it.copy(targetNumbers = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) }
    }
    
    fun updateWebhookUrl(value: String) {
        _uiState.update { it.copy(webhookUrl = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.WEBHOOK_URL, value) }
    }
    
    fun updateForwardPhone(value: String) {
        _uiState.update { it.copy(forwardPhone = value, targetNumbers = value) }
        viewModelScope.launch { 
            settingsRepository.updateString(SettingsRepository.FORWARD_PHONE, value) 
            settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) 
        }
    }
    
    fun updateWebhookFilter(value: String) {
        _uiState.update { it.copy(webhookFilter = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.WEBHOOK_FILTER, value) }
    }
    
    fun updateAllowExternalAutomation(value: Boolean) {
        _uiState.update { it.copy(allowExternalAutomation = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALLOW_EXTERNAL_AUTOMATION, value) }
    }
    
    fun updateAutoRespondMissedCall(value: Boolean) {
        _uiState.update { it.copy(autoRespondMissedCall = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_MISSED_CALL, value) }
    }
    
    fun updateAutoRespondSms(value: Boolean) {
        _uiState.update { it.copy(autoRespondSms = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_SMS, value) }
    }
    
    fun updateSilentSwallow(value: Boolean) {
        _uiState.update { it.copy(silentSwallow = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SILENT_SWALLOW, value) }
    }
    
    fun updateMasterKillSwitch(value: Boolean) {
        _uiState.update { it.copy(masterKillSwitch = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.MASTER_KILL_SWITCH, value) }
    }
    
    fun updateSmsForwardingEnabled(value: Boolean) {
        _uiState.update { it.copy(smsForwardingEnabled = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SMS_FORWARDING_ENABLED, value) }
    }

    fun updateSenders(value: String) {
        _uiState.update { it.copy(senders = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SENDERS, value) }
    }

    fun updateKeywordFilter(value: String) {
        _uiState.update { it.copy(keywordFilter = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.KEYWORD_FILTER, value) }
    }
    
    fun updateMerchantKeywords(value: String) {
        _uiState.update { it.copy(merchantKeywords = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.MERCHANT_KEYWORDS, value) }
    }

    fun updateScamKeywords(value: String) {
        _uiState.update { it.copy(scamKeywords = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SCAM_KEYWORDS, value) }
    }
    
    fun updateVipCallers(value: String) {
        _uiState.update { it.copy(vipCallers = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_CALLERS, value) }
    }
    
    fun updateAutoForwardCalls(value: Boolean) {
        _uiState.update { it.copy(autoForwardCalls = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_FORWARD_CALLS, value) }
    }
    
    fun updateCallForwardTarget(value: String) {
        _uiState.update { it.copy(callForwardTarget = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.CALL_FORWARD_TARGET, value) }
    }

    fun updateAutoForwardDuration(value: String) {
        _uiState.update { it.copy(autoForwardDuration = value) }
        val asInt = value.toIntOrNull() ?: 5
        viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.AUTO_FORWARD_DURATION, asInt) }
    }

    fun updateAlertForwardTarget(value: Boolean) {
        _uiState.update { it.copy(alertForwardTarget = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALERT_FORWARD_TARGET, value) }
    }
    
    fun updateVipDivertNumber(value: String) {
        _uiState.update { it.copy(vipDivertNumber = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_DIVERT_NUMBER, value) }
    }
    
    fun updateOverrideDnd(value: Boolean) {
        _uiState.update { it.copy(overrideDnd = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.OVERRIDE_DND, value) }
    }
    
    fun updateDndTimeframeMinutes(value: String) {
        _uiState.update { it.copy(dndTimeframeMinutes = value) }
        val asInt = value.toIntOrNull() ?: 5
        viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_TIMEFRAME_MINUTES, asInt) }
    }
    
    fun updateDndThresholdCalls(value: String) {
        _uiState.update { it.copy(dndThresholdCalls = value) }
        val asInt = value.toIntOrNull() ?: 2
        viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_THRESHOLD_CALLS, asInt) }
    }
    
    fun updateDetectBusyAndReply(value: Boolean) {
        _uiState.update { it.copy(detectBusyAndReply = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.DETECT_BUSY, value) }
    }
    
    fun updateBusyReplyMessage(value: String) {
        _uiState.update { it.copy(busyReplyMessage = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.BUSY_REPLY_MSG, value) }
    }

    fun updateSelectedSimId(value: String?) {
        _uiState.update { it.copy(selectedSimId = value) }
        viewModelScope.launch {
            if (value == null) {
                settingsRepository.removeKey(SettingsRepository.SELECTED_SIM_ID)
            } else {
                settingsRepository.updateString(SettingsRepository.SELECTED_SIM_ID, value)
            }
        }
    }

    fun updateShowKjCompanion(value: Boolean) {
        _uiState.update { it.copy(showKjCompanion = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_KJ_COMPANION, value) }
    }
    
    fun updateShowKjAi(value: Boolean) {
        _uiState.update { it.copy(showKjAi = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_KJ_AI, value) }
    }
    
    fun updateShowCalls(value: Boolean) {
        _uiState.update { it.copy(showCalls = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_CALLS, value) }
    }
    
    fun updateShowShield(value: Boolean) {
        _uiState.update { it.copy(showShield = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_SHIELD, value) }
    }
    
    fun updateShowDeclutter(value: Boolean) {
        _uiState.update { it.copy(showDeclutter = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_DECLUTTER, value) }
    }
    
    fun updateWidgetRecentLogs(value: Boolean) {
        _uiState.update { it.copy(widgetRecentLogs = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_RECENT_LOGS, value) }
    }
    
    fun updateWidgetQuickChat(value: Boolean) {
        _uiState.update { it.copy(widgetQuickChat = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_QUICK_CHAT, value) }
    }

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
