package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
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

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.KjAiViewModel
import androidx.fragment.app.FragmentActivity
import com.example.utils.SecurityUtils
import android.widget.Toast

import com.example.ShieldApplication

import com.example.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KjAiScreen(initialDeepLinkRule: String? = null, viewModel: KjAiViewModel = viewModel(factory = KjAiViewModel.Factory(LocalContext.current, (LocalContext.current.applicationContext as ShieldApplication).container.ruleRepository))) {
    val context = LocalContext.current
    val settingsRepository = (context.applicationContext as ShieldApplication).container.settingsRepository
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(settingsRepository))
    val settingsState by settingsViewModel.uiState.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val engineType = uiState.engineType
    val customEndpoint = uiState.customEndpoint
    val customApiKey = uiState.customApiKey
    val provider = uiState.provider
    val systemPrompt = uiState.systemPrompt
    
    var apiKeyVisible by remember { mutableStateOf(false) }
    var isVaultUnlocked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "KJ AI",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "KJ Settings", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold 
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Configure how your personal KJ assistant thinks and processes incoming events.",
                style = MaterialTheme.typography.bodyLarge,
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
                        viewModel.updateEngineType("LOCAL")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                EngineOptionCard(
                    title = "BYOK Cloud API (Smartest)",
                    description = "Bring Your Own Key vault for OpenAI, Anthropic, Gemini, or custom MCP endpoint.",
                    icon = Icons.Default.Cloud,
                    isSelected = engineType == "API",
                    onClick = { 
                        viewModel.updateEngineType("API")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (engineType == "API") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("BYOK Vault Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        if (!isVaultUnlocked) {
                            Button(
                                onClick = {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        SecurityUtils.authenticate(
                                            activity = activity,
                                            title = "Unlock BYOK Vault",
                                            subtitle = "Verify identity to access API Keys",
                                            onSuccess = { isVaultUnlocked = true },
                                            onError = { Toast.makeText(context, "Auth Failed: $it", Toast.LENGTH_SHORT).show() }
                                        )
                                    } else {
                                        Toast.makeText(context, "Security context not available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unlock")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock to Configure")
                            }
                        } else {
                            // Provider Selection
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("OpenAI", "Anthropic", "Google Gemini", "Groq", "DeepSeek", "Custom MCP").forEach { prov ->
                                    FilterChip(
                                        selected = provider == prov,
                                        onClick = { 
                                            viewModel.updateProvider(prov)
                                        },
                                        label = { Text(prov) }
                                    )
                                }
                            }
                        
                        if (uiState.availableModels.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedModel,
                                    onValueChange = { viewModel.updateSelectedModel(it) },
                                    readOnly = false,
                                    label = { Text("Model (${uiState.provider})") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    uiState.availableModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                viewModel.updateSelectedModel(model)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { 
                                viewModel.updateCustomEndpoint(it)
                            },
                            label = { Text("API Endpoint URL") },
                            placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { 
                                viewModel.updateCustomApiKey(it)
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
            }

            // Custom Prompt Editor
            Spacer(modifier = Modifier.height(8.dp))
            Text("Custom Prompt Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Tell the AI how to process intercepted text (System Prompt).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { 
                            viewModel.updateSystemPrompt(it)
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("POST extracted data to webhook, or forward via SMS to a destination phone number.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    var showWebhooksDialog by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = settingsState.forwardPhone,
                        onValueChange = { settingsViewModel.updateForwardPhone(it) },
                        label = { Text("Single Destination SMS Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Button(
                        onClick = { showWebhooksDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Webhook, contentDescription = "Webhooks")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Advanced Webhooks")
                    }

                    if (showWebhooksDialog) {
                        ManageWebhooksDialog(
                            webhookRepository = (LocalContext.current.applicationContext as ShieldApplication).container.webhookRepository,
                            onDismiss = { showWebhooksDialog = false }
                        )
                    }

                    OutlinedTextField(
                        value = settingsState.webhookUrl,
                        onValueChange = { settingsViewModel.updateWebhookUrl(it) },
                        label = { Text("Webhook POST URL") },
                        placeholder = { Text("https://hooks.zapier.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text("Trigger Webhook On:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "OTP", "TRANSACTION", "MISSED_CALL").forEach { filterOption ->
                            FilterChip(
                                selected = settingsState.webhookFilter == filterOption,
                                onClick = { settingsViewModel.updateWebhookFilter(filterOption) },
                                label = { Text(filterOption) }
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text("Allow External Automation Broadcasts", fontWeight = FontWeight.SemiBold)
                            Text("Send intents to Tasker/MacroDroid (com.example.shield.RULE_TRIGGERED)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settingsState.allowExternalAutomation,
                            onCheckedChange = { settingsViewModel.updateAllowExternalAutomation(it) }
                        )
                    }
                }
            }

            // Export JSON
            var showRuleBuilderDialog by remember { mutableStateOf(false) }
            var newTrigger by remember { mutableStateOf("") }
            var newAction by remember { mutableStateOf("") }
            var showTaskerHelp by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(8.dp))
            var showImportDialog by remember { mutableStateOf(false) }
            var importJsonText by remember { mutableStateOf("") }
            
            LaunchedEffect(initialDeepLinkRule) {
                if (!initialDeepLinkRule.isNullOrBlank()) {
                    try {
                        val decodedBytes = android.util.Base64.decode(initialDeepLinkRule, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                        importJsonText = String(decodedBytes)
                        showImportDialog = true
                    } catch (e: Exception) {
                        importJsonText = initialDeepLinkRule
                        showImportDialog = true
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Saved Triggers & Recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showTaskerHelp = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Tasker Help")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Import JSON")
                    }
                    IconButton(onClick = { 
                        // Real Export logic
                        val customRulesArray = org.json.JSONArray()
                        uiState.customRules.forEach { r ->
                            val obj = org.json.JSONObject()
                            obj.put("trigger", r.trigger)
                            obj.put("action", r.action)
                            customRulesArray.put(obj)
                        }
                        
                        val rulesJson = """
                        {
                          "engine_type": "$engineType",
                          "api_provider": "$provider",
                          "rules": $customRulesArray,
                          "metadata": {
                              "attribution": "Created via Shield App",
                              "version": "1.0"
                          }
                        }
                        """.trimIndent()
                        
                        val base64Str = android.util.Base64.encodeToString(rulesJson.toByteArray(), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                        val shareUrl = "shield-app://import?rule=$base64Str"
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out my Shield App rules!\n\n$shareUrl")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Rules"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Link")
                    }
                    IconButton(onClick = { 
                        // Real Export logic
                        val customRulesArray = org.json.JSONArray()
                        uiState.customRules.forEach { r ->
                            val obj = org.json.JSONObject()
                            obj.put("trigger", r.trigger)
                            obj.put("action", r.action)
                            customRulesArray.put(obj)
                        }
                        
                        val rulesJson = """
                        {
                          "engine_type": "$engineType",
                          "api_provider": "$provider",
                          "rules": $customRulesArray,
                          "metadata": {
                              "attribution": "Created via Shield App",
                              "version": "1.0"
                          }
                        }
                        """.trimIndent()
                        val filename = "KjAi_Rules_${System.currentTimeMillis()}.shield"
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

            if (showTaskerHelp) {
                AlertDialog(
                    onDismissRequest = { showTaskerHelp = false },
                    title = { Text("Tasker/MacroDroid Integration") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("You can intercept events using Android's broadcast system.", style = MaterialTheme.typography.bodyMedium)
                            Text("Action:", fontWeight = FontWeight.Bold)
                            Text("com.example.shield.RULE_TRIGGERED", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("Extras (Variables):", fontWeight = FontWeight.Bold)
                            Text("• type (String)\n• title (String) - Sender\n• message (String) - Body\n• rule_name (String)", style = MaterialTheme.typography.bodySmall)
                            Text("Ensure the 'Allow External Automation Broadcasts' switch is ON in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTaskerHelp = false }) { Text("Got it") }
                    }
                )
            }

            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Import JSON Rules") },
                    text = {
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("Paste JSON here...") },
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                val jsonObj = org.json.JSONObject(importJsonText)
                                val rulesArray = jsonObj.getJSONArray("rules")
                                var importedCount = 0
                                for (i in 0 until rulesArray.length()) {
                                    val r = rulesArray.getJSONObject(i)
                                    val trigger = r.optString("trigger", "").trim().take(100)
                                    val action = r.optString("action", "").trim().take(200)
                                    
                                    val sanitizedTrigger = trigger.replace(Regex("<[^>]*>"), "")
                                    val sanitizedAction = action.replace(Regex("<[^>]*>"), "")

                                    if (sanitizedTrigger.isNotBlank() && sanitizedAction.isNotBlank()) {
                                        viewModel.addCustomRule(sanitizedTrigger, sanitizedAction)
                                        importedCount++
                                    }
                                }
                                showImportDialog = false
                                android.widget.Toast.makeText(context, "Imported $importedCount valid rules", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Invalid JSON", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Import") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showRuleBuilderDialog) {
                AlertDialog(
                    onDismissRequest = { showRuleBuilderDialog = false },
                    title = { Text("Add Custom Rule") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Trigger Condition", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newTrigger,
                                onValueChange = { newTrigger = it },
                                placeholder = { Text("e.g. If SMS contains 'Bank'") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Action", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newAction,
                                onValueChange = { newAction = it },
                                placeholder = { Text("e.g. POST to Webhook") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(onClick = { newAction = "Forward via SMS" }, label = { Text("SMS") })
                                SuggestionChip(onClick = { newAction = "POST to Webhook" }, label = { Text("Webhook") })
                                SuggestionChip(onClick = { newAction = "Trigger Tasker Intent" }, label = { Text("Tasker Intent") })
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newTrigger.isNotBlank() && newAction.isNotBlank()) {
                                viewModel.addCustomRule(newTrigger, newAction)
                                newTrigger = ""
                                newAction = ""
                                showRuleBuilderDialog = false
                            }
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRuleBuilderDialog = false }) { Text("Cancel") }
                    }
                )
            }

            uiState.customRules.forEach { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = "Rule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.trigger, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Then ${rule.action}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f))
                        }
                        IconButton(onClick = { 
                            val rulesJson = """
                            {
                              "engine_type": "$engineType",
                              "api_provider": "$provider",
                              "rules": [
                                { "trigger": "${rule.trigger}", "action": "${rule.action}" }
                              ],
                              "metadata": {
                                  "attribution": "Created via Shield App",
                                  "version": "1.0"
                              }
                            }
                            """.trimIndent()
                            
                            val base64Str = android.util.Base64.encodeToString(rulesJson.toByteArray(), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                            val shareUrl = "shield-app://import?rule=$base64Str"
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out my Shield App rule!\n\n$shareUrl")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Rule"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.removeCustomRule(rule) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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

            val autoRespondSmsColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (settingsState.autoRespondSms) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = androidx.compose.animation.core.tween(300)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = autoRespondSmsColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoMode, contentDescription = "Rule", tint = if (settingsState.autoRespondSms) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("If Unknown SMS arrives", fontWeight = FontWeight.Bold, color = if (settingsState.autoRespondSms) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                        Text("Then Auto-Respond via SMS using KJ Engine", style = MaterialTheme.typography.bodySmall, color = if (settingsState.autoRespondSms) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settingsState.autoRespondSms,
                        onCheckedChange = { settingsViewModel.updateAutoRespondSms(it) }
                    )
                }
            }

            val autoRespondCallColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (settingsState.autoRespondMissedCall) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = androidx.compose.animation.core.tween(300)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = autoRespondCallColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoMode, contentDescription = "Rule", tint = if (settingsState.autoRespondMissedCall) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("If Missed Call from Unknown", fontWeight = FontWeight.Bold, color = if (settingsState.autoRespondMissedCall) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                        Text("Then Auto-Respond via SMS using KJ Engine", style = MaterialTheme.typography.bodySmall, color = if (settingsState.autoRespondMissedCall) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settingsState.autoRespondMissedCall,
                        onCheckedChange = { settingsViewModel.updateAutoRespondMissedCall(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun EngineOptionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "engineContainerColor"
    )
    val iconBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha=0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.1f),
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "engineIconBgColor"
    )
    val iconTintColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "engineIconTintColor"
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "engineTextColor"
    )
    val descColor by androidx.compose.animation.animateColorAsState(
        targetValue = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "engineDescColor"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTintColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = descColor)
            }
        }
    }
}
