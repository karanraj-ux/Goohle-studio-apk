package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SmsLogEntity
import com.example.data.SubscriptionEntity
import com.example.db.CustomRule
import com.example.data.repository.FinancialRepository
import com.example.data.repository.RuleRepository
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.flow.combine

data class MainUiState(
    val recentLogs: List<SmsLogEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val expenses: List<com.example.data.ExpenseEntity> = emptyList(),
    val rules: List<CustomRule> = emptyList(),
    val totalSpent: Double = 0.0,
    val forwardedToday: Int = 0,
    val totalForwarded: Int = 0
)

class MainViewModel(
    private val smsRepository: SmsRepository,
    private val financialRepository: FinancialRepository,
    private val ruleRepository: RuleRepository
) : ViewModel() {
    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MainUiState> = combine(
        combine(
            smsRepository.getRecentLogs(),
            financialRepository.getFinancialSubscriptions(),
            financialRepository.getAllExpenses(),
            ruleRepository.allRules
        ) { a, b, c, d -> arrayOf(a, b, c, d) },
        combine(
            financialRepository.getTotalSpentFlow(),
            smsRepository.getForwardedCountToday(todayStart),
            smsRepository.getTotalForwardedCount()
        ) { a, b, c -> Triple(a, b, c) }
    ) { arr, (spent, fwToday, fwTotal) ->
        MainUiState(
            recentLogs = arr[0] as List<SmsLogEntity>,
            subscriptions = arr[1] as List<SubscriptionEntity>,
            expenses = arr[2] as List<com.example.data.ExpenseEntity>,
            rules = arr[3] as List<CustomRule>,
            totalSpent = spent ?: 0.0,
            forwardedToday = fwToday,
            totalForwarded = fwTotal
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, MainUiState())

    fun clearLogs() {
        viewModelScope.launch {
            smsRepository.clearLogs()
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            financialRepository.deleteSubscriptionById(id)
        }
    }

    fun addRule(trigger: String, action: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ruleRepository.insertRule(CustomRule(trigger = trigger, action = action))
        }
    }

    fun deleteRule(rule: CustomRule) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ruleRepository.deleteRule(rule)
        }
    }

    class Factory(
        private val smsRepo: SmsRepository,
        private val finRepo: FinancialRepository,
        private val ruleRepo: RuleRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(smsRepo, finRepo, ruleRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
