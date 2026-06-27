package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallJobEntity
import com.example.data.repository.CallJobRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallsUiState(
    val activeCallJobs: List<CallJobEntity> = emptyList()
)

class CallsViewModel(
    private val callJobRepository: CallJobRepository
) : ViewModel() {

    val uiState: StateFlow<CallsUiState> = callJobRepository.getAllJobsFlow()
        .map { CallsUiState(activeCallJobs = it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, CallsUiState())

    fun insertCallJob(job: CallJobEntity, onInserted: (Long) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val id = callJobRepository.insert(job)
            launch(kotlinx.coroutines.Dispatchers.Main) {
                onInserted(id)
            }
        }
    }
    
    fun deleteCallJob(id: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            callJobRepository.deleteById(id)
        }
    }

    class Factory(
        private val callRepo: CallJobRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CallsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CallsViewModel(callRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
