package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallJobEntity
import com.example.data.SmsLogEntity
import com.example.data.SubscriptionEntity
import com.example.data.repository.CallJobRepository
import com.example.data.repository.FinancialRepository
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
    val totalSpent: Double = 0.0,
    val forwardedToday: Int = 0,
    val totalForwarded: Int = 0
)

class MainViewModel(
    private val smsRepository: SmsRepository,
    private val callJobRepository: CallJobRepository,
    private val financialRepository: FinancialRepository
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

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            smsRepository.getRecentLogs(),
            financialRepository.getFinancialSubscriptions(),
            financialRepository.getAllExpenses()
        ) { a, b, c -> Triple(a, b, c) },
        combine(
            financialRepository.getTotalSpentFlow(),
            smsRepository.getForwardedCountToday(todayStart),
            smsRepository.getTotalForwardedCount()
        ) { a, b, c -> Triple(a, b, c) }
    ) { (logs, subs, expenses), (spent, fwToday, fwTotal) ->
        MainUiState(
            recentLogs = logs,
            subscriptions = subs,
            expenses = expenses,
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
    
    fun scanFinancialData(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.declutter.SmsFinancialScanner.scanSmsForSubscriptions(context)
        }
    }
    
    fun deleteSubscription(id: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            financialRepository.deleteSubscriptionById(id)
        }
    }

    class Factory(
        private val smsRepo: SmsRepository,
        private val callRepo: CallJobRepository,
        private val finRepo: FinancialRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(smsRepo, callRepo, finRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
