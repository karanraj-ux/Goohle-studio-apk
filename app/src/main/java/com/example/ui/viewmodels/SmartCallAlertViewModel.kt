package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallJobEntity
import com.example.data.repository.CallJobRepository
import kotlinx.coroutines.launch

class SmartCallAlertViewModel(
    private val callJobRepository: CallJobRepository
) : ViewModel() {

    fun scheduleCall(phoneNumber: String, onInsertCall: (CallJobEntity) -> Unit) {
        viewModelScope.launch {
            val job = CallJobEntity(
                phoneNumber = phoneNumber,
                totalCalls = 3,
                intervalMinutes = 5,
                nextCallTime = System.currentTimeMillis() + (2 * 60 * 1000), // Next call in 2 mins
                description = "Triggered from Smart Alert"
            )
            val id = callJobRepository.insert(job)
            onInsertCall(job.copy(id = id))
        }
    }

    class Factory(private val callJobRepository: CallJobRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmartCallAlertViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SmartCallAlertViewModel(callJobRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
