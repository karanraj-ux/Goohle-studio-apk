package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SmsLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.smsLogDao()

    val recentLogs: StateFlow<List<SmsLogEntity>> = dao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val forwardedToday: StateFlow<Int> = dao.getForwardedCountToday(todayStart)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalForwarded: StateFlow<Int> = dao.getTotalForwardedCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearLogs()
        }
    }
}
