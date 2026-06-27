package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessageEntity
import com.example.data.repository.CallJobRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FinancialRepository
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KjCompanionUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val calls: List<com.example.data.CallJobEntity> = emptyList(),
    val sms: List<com.example.data.SmsLogEntity> = emptyList(),
    val expenses: List<com.example.data.ExpenseEntity> = emptyList(),
    val subs: List<com.example.data.SubscriptionEntity> = emptyList()
)

class KjCompanionViewModel(
    private val chatRepository: ChatRepository,
    private val callJobRepository: CallJobRepository,
    private val smsRepository: SmsRepository,
    private val financialRepository: FinancialRepository
) : ViewModel() {

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError = _apiError.asStateFlow()

    val uiState: StateFlow<KjCompanionUiState> = combine(
        combine(
            chatRepository.getAllMessages(),
            callJobRepository.getAllJobsFlow(),
            smsRepository.getRecentLogs()
        ) { a, b, c -> Triple(a, b, c) },
        combine(
            financialRepository.getAllExpenses(),
            financialRepository.getFinancialSubscriptions()
        ) { a, b -> Pair(a, b) }
    ) { (msgs, calls, sms), (expenses, subs) ->
        KjCompanionUiState(
            messages = msgs,
            calls = calls,
            sms = sms,
            expenses = expenses,
            subs = subs
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, KjCompanionUiState())

    fun clearAllMessages() {
        viewModelScope.launch {
            chatRepository.clearAllMessages()
        }
    }

    fun insertMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            chatRepository.insertMessage(message)
        }
    }

    fun sendMessage(userText: String, context: android.content.Context) {
        viewModelScope.launch {
            chatRepository.insertMessage(ChatMessageEntity(text = userText, isUser = true, timestamp = System.currentTimeMillis()))
            
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            if (settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.MASTER_KILL_SWITCH, false)) {
                chatRepository.insertMessage(ChatMessageEntity(text = "Master Kill Switch is active. AI processing is disabled.", isUser = false, timestamp = System.currentTimeMillis()))
                return@launch
            }
            
            val dbUtility = com.example.data.KjDatabaseUtility(context)
            val logsContext = dbUtility.getAggregateLogsForAI()
            
            val aiPrefs = com.example.utils.SecurityUtils.getEncryptedPrefs(context, "kj_ai_prefs")
            val apiKey = aiPrefs.getString("custom_api_key", "") ?: ""
            val model = aiPrefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
            val provider = aiPrefs.getString("api_provider", "Google Gemini") ?: "Google Gemini"

            _apiError.value = null
            _isTyping.value = true
            val responseText = try {
                com.example.network.generateContentWithHistory(
                    history = uiState.value.messages,
                    prompt = userText,
                    contextData = logsContext,
                    customModel = model,
                    customApiKey = apiKey,
                    provider = provider,
                    context = context
                )
            } catch (e: Exception) {
                _apiError.value = e.message ?: "An unknown error occurred"
                null
            } finally {
                _isTyping.value = false
            }
            if (responseText != null) {
                chatRepository.insertMessage(ChatMessageEntity(text = responseText, isUser = false, timestamp = System.currentTimeMillis()))
            }
        }
    }

    fun dismissError() {
        _apiError.value = null
    }

    class Factory(
        private val chatRepo: ChatRepository,
        private val callRepo: CallJobRepository,
        private val smsRepo: SmsRepository,
        private val finRepo: FinancialRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(KjCompanionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return KjCompanionViewModel(chatRepo, callRepo, smsRepo, finRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
