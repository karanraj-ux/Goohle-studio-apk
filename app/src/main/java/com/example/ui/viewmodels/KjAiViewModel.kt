package com.example.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import androidx.lifecycle.viewModelScope
import com.example.network.detectAvailableModels
import kotlinx.coroutines.launch
import com.example.ShieldApplication
import com.example.db.CustomRule
import com.example.data.repository.RuleRepository

data class KjAiUiState(
    val engineType: String = "LOCAL",
    val customEndpoint: String = "",
    val customApiKey: String = "",
    val provider: String = "OpenAI",
    val systemPrompt: String = "Summarize this long email notification into 3 bullet points.",
    val selectedModel: String = "gemini-2.5-flash",
    val availableModels: List<String> = emptyList(),
    val customRules: List<CustomRule> = emptyList()
)

class KjAiViewModel(
    private val context: Context,
    private val ruleRepository: RuleRepository
) : ViewModel() {
    private var prefs: SharedPreferences? = null

    private val _uiState = MutableStateFlow(KjAiUiState())
    val uiState: StateFlow<KjAiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            prefs = com.example.utils.SecurityUtils.getEncryptedPrefs(context, "kj_ai_prefs")
            val p = prefs!!
            val initialApiKey = p.getString("custom_api_key", "") ?: ""
            
            _uiState.update { 
                it.copy(
                    engineType = p.getString("engine_type", "LOCAL") ?: "LOCAL",
                    customEndpoint = p.getString("custom_endpoint", "") ?: "",
                    customApiKey = initialApiKey,
                    provider = p.getString("api_provider", "OpenAI") ?: "OpenAI",
                    systemPrompt = p.getString("system_prompt", "Summarize this long email notification into 3 bullet points.") ?: "",
                    selectedModel = p.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
                )
            }
            if (initialApiKey.isNotBlank()) {
                refreshAvailableModels(initialApiKey)
            }
            
            // Collect rules from repository
            ruleRepository.allRules.collect { rules ->
                _uiState.update { it.copy(customRules = rules) }
            }
        }
    }

    fun addCustomRule(trigger: String, action: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ruleRepository.insertRule(CustomRule(trigger = trigger, action = action))
        }
    }

    fun removeCustomRule(rule: CustomRule) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ruleRepository.deleteRule(rule)
        }
    }

    private suspend fun refreshAvailableModels(apiKey: String) {
        try {
            var modelNames = emptyList<String>()
            var autoProvider = "OpenAI"
            if (apiKey.startsWith("gsk_")) {
                autoProvider = "Groq"
                modelNames = listOf("llama-3.3-70b-versatile", "llama3-8b-8192", "mixtral-8x7b-32768", "gemma2-9b-it", "qwen3-32b")
            } else if (apiKey.startsWith("sk-ant")) {
                autoProvider = "Anthropic"
                modelNames = listOf("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307", "claude-3-opus-20240229")
            } else if (apiKey.startsWith("AIza")) {
                autoProvider = "Google Gemini"
                try {
                    val liveModels = com.example.network.detectAvailableModels(apiKey)
                    if (liveModels.isNotEmpty()) {
                        modelNames = liveModels.map { it.name.removePrefix("models/") }
                    } else {
                        modelNames = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash", "gemini-2.5-flash")
                    }
                } catch(e: Exception) {
                    modelNames = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash", "gemini-2.5-flash")
                }
            } else if (apiKey.startsWith("sk-")) {
                autoProvider = "OpenAI"
                modelNames = listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo")
            }

            if (modelNames.isNotEmpty()) {
                _uiState.update { it.copy(availableModels = modelNames, provider = autoProvider) }
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("api_provider", autoProvider)?.commit() }
                // Select a default good one if currently selected model is missing
                val current = _uiState.value.selectedModel
                if (!modelNames.contains(current)) {
                    val fallback = modelNames.first()
                    updateSelectedModel(fallback)
                }
            }
        } catch (e: Exception) {
            Log.e("KjAiViewModel", "Model detection failed", e)
        }
    }

    fun updateEngineType(type: String) {
        _uiState.update { it.copy(engineType = type) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("engine_type", type)?.commit() }
    }

    fun updateCustomEndpoint(endpoint: String) {
        _uiState.update { it.copy(customEndpoint = endpoint) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("custom_endpoint", endpoint)?.commit() }
    }

    fun updateCustomApiKey(key: String) {
        _uiState.update { it.copy(customApiKey = key) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("custom_api_key", key)?.commit() }
        viewModelScope.launch {
            if (key.isNotBlank()) refreshAvailableModels(key)
        }
    }

    fun updateProvider(prov: String) {
        _uiState.update { it.copy(provider = prov) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("api_provider", prov)?.commit() }
        
        val newModels = when (prov) {
            "Google Gemini" -> listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash", "gemini-2.5-flash")
            "OpenAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo")
            "Anthropic" -> listOf("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307", "claude-3-opus-20240229")
            "Groq" -> listOf("llama-3.3-70b-versatile", "llama3-8b-8192", "mixtral-8x7b-32768", "gemma2-9b-it", "qwen3-32b")
            "DeepSeek" -> listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
            else -> listOf("custom-model-1")
        }
        _uiState.update { it.copy(availableModels = newModels) }
        if (!newModels.contains(_uiState.value.selectedModel) && newModels.isNotEmpty()) {
            updateSelectedModel(newModels.first())
        }
    }

    fun updateSystemPrompt(prompt: String) {
        _uiState.update { it.copy(systemPrompt = prompt) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("system_prompt", prompt)?.commit() }
    }

    fun updateSelectedModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs?.edit()?.putString("selected_model", model)?.commit() }
    }

    class Factory(private val context: Context, private val ruleRepository: RuleRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(KjAiViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return KjAiViewModel(context.applicationContext, ruleRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

