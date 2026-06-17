package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KjAiScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kj_ai_prefs", Context.MODE_PRIVATE) }

    var engineType by remember { mutableStateOf(prefs.getString("engine_type", "LOCAL") ?: "LOCAL") }
    var customEndpoint by remember { mutableStateOf(prefs.getString("custom_endpoint", "") ?: "") }
    var customApiKey by remember { mutableStateOf(prefs.getString("custom_api_key", "") ?: "") }
    var provider by remember { mutableStateOf(prefs.getString("api_provider", "OpenAI") ?: "OpenAI") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var systemPrompt by remember { mutableStateOf(prefs.getString("system_prompt", "Summarize this long email notification into 3 bullet points.") ?: "") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("KJ AI Engine") })
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Configure how your personal KJ assistant thinks and processes incoming events like Calls, SMS, and Notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Select Engine Type
            Text("Select Brain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EngineOptionCard(
                    title = "Rules Engine (Fastest)",
                    description = "Fast, private, rules-based static engine running natively offline.",
                    icon = Icons.Default.FlashOn,
                    isSelected = engineType == "LOCAL",
                    onClick = { 
                        engineType = "LOCAL"
                        prefs.edit().putString("engine_type", "LOCAL").apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                EngineOptionCard(
                    title = "Local SLM Bridge (Experimental)",
                    description = "Hook for Small Language Models (MediaPipe/MLC) for 100% offline neural parsing.",
                    icon = Icons.Default.Memory,
                    isSelected = engineType == "SLM",
                    onClick = { 
                        engineType = "SLM"
                        prefs.edit().putString("engine_type", "SLM").apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                EngineOptionCard(
                    title = "BYOK Cloud API (Smartest)",
                    description = "Bring Your Own Key vault for OpenAI, Anthropic, Gemini, or custom MCP endpoint.",
                    icon = Icons.Default.Cloud,
                    isSelected = engineType == "API",
                    onClick = { 
                        engineType = "API"
                        prefs.edit().putString("engine_type", "API").apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (engineType == "API") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("BYOK Vault Configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Provider Selection
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("OpenAI", "Anthropic", "Gemini", "Custom MCP").forEach { prov ->
                                FilterChip(
                                    selected = provider == prov,
                                    onClick = { 
                                        provider = prov
                                        prefs.edit().putString("api_provider", prov).apply()
                                    },
                                    label = { Text(prov) }
                                )
                            }
                        }
                        
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { 
                                customEndpoint = it
                                prefs.edit().putString("custom_endpoint", it).apply()
                            },
                            label = { Text("API Endpoint URL") },
                            placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { 
                                customApiKey = it
                                prefs.edit().putString("custom_api_key", it).apply()
                            },
                            label = { Text("Secure API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle API Key visibility")
                                }
                            }
                        )
                    }
                }
            }

            // Custom Prompt Editor
            Spacer(modifier = Modifier.height(8.dp))
            Text("Custom Prompt Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tell the AI how to process intercepted text (System Prompt).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { 
                            systemPrompt = it
                            prefs.edit().putString("system_prompt", it).apply()
                        },
                        label = { Text("System Instructions") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            }

            // Forwarding & Webhook Configuration
            Spacer(modifier = Modifier.height(8.dp))
            Text("Forwarding & Webhook Output", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("POST extracted data to webhook, or forward via SMS to a destination phone number.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val appPrefsConfig = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                    var webhookUrl by remember { mutableStateOf(appPrefsConfig.getString("webhook_url", "") ?: "") }
                    var forwardPhone by remember { mutableStateOf(appPrefsConfig.getString("forward_phone", "") ?: "") }
                    val appPrefsSms = remember { context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE) }
                    
                    OutlinedTextField(
                        value = forwardPhone,
                        onValueChange = { 
                            forwardPhone = it
                            appPrefsConfig.edit().putString("forward_phone", it).apply()
                            appPrefsSms.edit().putString("target_numbers", it).apply()
                        },
                        label = { Text("Single Destination SMS Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { 
                            webhookUrl = it
                            appPrefsConfig.edit().putString("webhook_url", it).apply()
                        },
                        label = { Text("Webhook POST URL") },
                        placeholder = { Text("https://hooks.zapier.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Export JSON
            var showRuleBuilderDialog by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Saved Triggers & Recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { 
                        val rulesJson = """
                        {
                          "engine_type": "$engineType",
                          "api_provider": "$provider",
                          "system_prompt": "$systemPrompt",
                          "rules": [
                            {"trigger": "OTP", "action": "POST to Webhook & Forward via SMS"},
                            {"trigger": "Unknown SMS", "action": "Auto-Respond SMS"},
                            {"trigger": "Missed Call from Unknown", "action": "Auto-Respond SMS"}
                          ]
                        }
                        """.trimIndent()
                        val filename = "KjAi_Rules_${System.currentTimeMillis()}.json"
                        try {
                            val file = java.io.File(context.getExternalFilesDir(null), filename)
                            file.writeText(rulesJson)
                            android.widget.Toast.makeText(context, "Exported to: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Export JSON")
                    }
                    IconButton(onClick = { showRuleBuilderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule")
                    }
                }
            }

            if (showRuleBuilderDialog) {
                AlertDialog(
                    onDismissRequest = { showRuleBuilderDialog = false },
                    title = { Text("Add Custom Rule") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Trigger Condition", fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("e.g. If SMS contains 'Bank'") })
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Action", fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("e.g. POST to Webhook") })
                            Text("This is a placeholder for the advanced visual rule builder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRuleBuilderDialog = false }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRuleBuilderDialog = false }) { Text("Cancel") }
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = "Rule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("If OTP arrives", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Then POST to Webhook & Forward via SMS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f))
                    }
                }
            }

            val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            var autoRespondMissedCall by remember { mutableStateOf(appPrefs.getBoolean("auto_respond_missed_call", false)) }
            var autoRespondSms by remember { mutableStateOf(appPrefs.getBoolean("auto_respond_sms", false)) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoMode, contentDescription = "Rule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("If Unknown SMS arrives", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Then Auto-Respond via SMS using KJ Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f))
                    }
                    Switch(
                        checked = autoRespondSms,
                        onCheckedChange = {
                            autoRespondSms = it
                            appPrefs.edit().putBoolean("auto_respond_sms", it).apply()
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoMode, contentDescription = "Rule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("If Missed Call from Unknown", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Then Auto-Respond via SMS using KJ Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f))
                    }
                    Switch(
                        checked = autoRespondMissedCall,
                        onCheckedChange = {
                            autoRespondMissedCall = it
                            appPrefs.edit().putBoolean("auto_respond_missed_call", it).apply()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun EngineOptionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(description, style = MaterialTheme.typography.bodySmall, color = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f))
            }
        }
    }
}
