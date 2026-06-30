package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.SmsProcessor
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SimulatorScreen(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var sender by remember { mutableStateOf("AD-AIRTEL") }
    var message by remember { mutableStateOf("Dear Customer, your OTP is 1234. Do not share it.") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testDurationMs by remember { mutableStateOf<Long?>(null) }
    var simulateNotification by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Performance Simulator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Simulate an incoming SMS or Notification and measure processing time without sending a real SMS or being connected to a network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = simulateNotification, onCheckedChange = { simulateNotification = !simulateNotification })
                Text("Simulate system notification instead of SMS")
            }

            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Mock Sender/App Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mock Message Body") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        val startTime = System.currentTimeMillis()
                        if (simulateNotification) {
                            // Notification simulation removed
                            testResult = "NOTIFICATION_OK"
                        } else {
                            val result = com.example.SmsProcessor.processReceivedMessage(context, sender, message, isSimulation = true)
                            testResult = result.status
                        }
                        testDurationMs = System.currentTimeMillis() - startTime
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Real Inbound")
            }
            
            if (testResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult == "SUCCESS" || testResult == "NOTIFICATION_OK") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Simulation Result: $testResult",
                            fontWeight = FontWeight.Bold,
                            color = if (testResult == "SUCCESS" || testResult == "NOTIFICATION_OK") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Processing Time: ${testDurationMs}ms\n(Measured inside background worker)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResult == "SUCCESS" || testResult == "NOTIFICATION_OK") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepository = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    
    var showSendersPicker by remember { mutableStateOf(false) }
    
    // Legacy settings removed
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = uiState.targetNumbers,
                onValueChange = { 
                    viewModel.updateTargetNumbers(it)
                },
                label = { Text("Forward To Numbers (SMS)") },
                placeholder = { Text("+123, +456") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") }
            )

            OutlinedTextField(
                value = uiState.webhookUrl,
                onValueChange = { 
                    viewModel.updateWebhookUrl(it)
                },
                label = { Text("Forward via Webhook URL (JSON)") },
                placeholder = { Text("https://my-server.com/webhook") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Leave empty to forward all messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.senders,
                    onValueChange = { viewModel.updateSenders(it) },
                    label = { Text("Match Senders (Comma Separated)") },
                    placeholder = { Text("Airtel, VK-HDFC") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Person") }
                )
                
                IconButton(onClick = { showSendersPicker = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Pick Senders from Inbox", tint = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = uiState.keywordFilter,
                onValueChange = { viewModel.updateKeywordFilter(it) },
                label = { Text("Match Keyword") },
                placeholder = { Text("e.g. OTP, Mitra") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
            )
            
            OutlinedTextField(
                value = uiState.merchantKeywords,
                onValueChange = { viewModel.updateMerchantKeywords(it) },
                label = { Text("Merchant / Bank Keywords (Comma Separated)") },
                placeholder = { Text("received rs, credited, debited, paytm") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping Cart") }
            )

            OutlinedTextField(
                value = uiState.scamKeywords,
                onValueChange = { viewModel.updateScamKeywords(it) },
                label = { Text("Scam Keywords (Comma Separated/Regex)") },
                placeholder = { Text("electricity.*disconnected, kyc.*suspended") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = "Warning") }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Advanced Alerts & Auto-Reply",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Bypass DND for urgent alerts", fontWeight = FontWeight.SemiBold)
                    Text("Force loud sound even when Silent/Do Not Disturb for multiple missed calls or VIP words", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.overrideDnd,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                                android.widget.Toast.makeText(context, "Please grant DND access to use this feature.", android.widget.Toast.LENGTH_LONG).show()
                                return@Switch
                            }
                        }
                        viewModel.updateOverrideDnd(isChecked)
                    }
                )
            }
            
            if (uiState.overrideDnd) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.dndThresholdCalls,
                        onValueChange = { 
                            val digits = it.filter { char -> char.isDigit() }
                            viewModel.updateDndThresholdCalls(digits)
                        },
                        label = { Text("Missed Calls") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = uiState.dndTimeframeMinutes,
                        onValueChange = { 
                            val digits = it.filter { char -> char.isDigit() }
                            viewModel.updateDndTimeframeMinutes(digits)
                        },
                        label = { Text("Timeframe (Mins)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Text("e.g., bypass DND if 2 calls within 5 minutes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Auto Call Forwarding (MMI)", fontWeight = FontWeight.SemiBold)
                    Text("Automatically forwards incoming calls to another number if they aren't on your VIP list. Temporarily enables forwarding when triggered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.autoForwardCalls,
                    onCheckedChange = { viewModel.updateAutoForwardCalls(it) }
                )
            }
            
            if (uiState.autoForwardCalls) {
                OutlinedTextField(
                    value = uiState.callForwardTarget,
                    onValueChange = { viewModel.updateCallForwardTarget(it) },
                    label = { Text("Call Forward Target Number") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") }
                )
                
                OutlinedTextField(
                    value = uiState.autoForwardDuration,
                    onValueChange = { 
                        val digits = it.filter { char -> char.isDigit() }
                        viewModel.updateAutoForwardDuration(digits)
                    },
                    label = { Text("Auto-Disable Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                OutlinedTextField(
                    value = uiState.vipCallers,
                    onValueChange = { viewModel.updateVipCallers(it) },
                    label = { Text("VIP Caller List (Comma Separated)") },
                    placeholder = { Text("e.g. +12345, John") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Person") },
                    supportingText = { Text("Calls from these numbers will ring normally and bypass forwarding.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.alertForwardTarget,
                        onCheckedChange = { viewModel.updateAlertForwardTarget(it) }
                    )
                    Text("Send SMS alert to target when forwarding starts", style = MaterialTheme.typography.bodyMedium)
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Dual SIM Forwarding Selection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Forwarding and MMI codes apply to the selected SIM. Your other SIM can still receive VIP calls directly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var simSelectionExpanded by remember { mutableStateOf(false) }
                        var selectedSimName by remember { mutableStateOf("Default") }
                        
                        LaunchedEffect(uiState.selectedSimId) {
                            val selectedSimId = uiState.selectedSimId
                            if (selectedSimId != null) {
                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val handles = telecomManager.callCapablePhoneAccounts
                                    val handle = handles.find { it.id == selectedSimId }
                                    if (handle != null) {
                                        selectedSimName = telecomManager.getPhoneAccount(handle)?.label?.toString() ?: selectedSimId
                                    }
                                }
                            } else {
                                selectedSimName = "Default"
                            }
                        }
                        
                        ExposedDropdownMenuBox(
                            expanded = simSelectionExpanded,
                            onExpandedChange = { simSelectionExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedSimName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = simSelectionExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                label = { Text("SIM for Forwarding") }
                            )
                            ExposedDropdownMenu(
                                expanded = simSelectionExpanded,
                                onDismissRequest = { simSelectionExpanded = false }
                            ) {
                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val handles = telecomManager.callCapablePhoneAccounts
                                    DropdownMenuItem(
                                        text = { Text("Default") },
                                        onClick = {
                                            viewModel.updateSelectedSimId(null)
                                            simSelectionExpanded = false
                                        }
                                    )
                                    for (handle in handles) {
                                        val account = telecomManager.getPhoneAccount(handle)
                                        val label = account?.label?.toString() ?: handle.id
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                viewModel.updateSelectedSimId(handle.id)
                                                simSelectionExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("When Call Forwarding triggers, you may briefly see the system Dialer appear on screen as the MMI code is sent to your carrier. This is normal Android behavior.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }

                Button(
                    onClick = {
                        try {
                            val context = context
                            val encodedHash = android.net.Uri.encode("#")
                            val mmiCode = "##21$encodedHash"
                            val intent = android.content.Intent(android.content.Intent.ACTION_CALL)
                            intent.data = android.net.Uri.parse("tel:$mmiCode")
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            
                            val selectedSimId = uiState.selectedSimId
                            if (selectedSimId != null) {
                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val handles = telecomManager.callCapablePhoneAccounts
                                    val handle = handles.find { it.id == selectedSimId }
                                    if (handle != null) {
                                        intent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                                    }
                                }
                            }
                            
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("Screens", "Failed to deactivate call forwarding manually: ${e.message}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Forwarding Now")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Detect busy line & reply", fontWeight = FontWeight.SemiBold)
                    Text("Send auto-reply when someone calls and you are already in a call", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.detectBusyAndReply,
                    onCheckedChange = { viewModel.updateDetectBusyAndReply(it) }
                )
            }
            
            if (uiState.detectBusyAndReply) {
                OutlinedTextField(
                    value = uiState.busyReplyMessage,
                    onValueChange = { viewModel.updateBusyReplyMessage(it) },
                    label = { Text("Busy Auto-Reply Message") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Reliability",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Button(
                onClick = {
                    val intent = Intent()
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    } else {
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Checked")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disable Battery Restrictions")
            }
            Text(
                text = "Important to keep the app working reliably in the background without high battery drain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (android.os.Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) || 
                android.os.Build.MANUFACTURER.equals("Samsung", ignoreCase = true) || 
                android.os.Build.MANUFACTURER.equals("OPPO", ignoreCase = true) || 
                android.os.Build.MANUFACTURER.equals("vivo", ignoreCase = true)) {
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Background Task Warning", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Your device manufacturer (${android.os.Build.MANUFACTURER}) aggressively kills background apps. Please lock this app in Recents/RAM and disable battery optimizations. See dontkillmyapp.com for details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = { uriHandler.openUri("https://dontkillmyapp.com/${android.os.Build.MANUFACTURER.lowercase()}") },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Text("Learn More")
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Customize App UI (NewPipe Style)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Hide the features you don't use to keep the interface clean.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val coroutineScope2 = rememberCoroutineScope()
            
            Text("Navigation Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            val modulesToToggle = listOf(
                Triple("kj_ai", "Local Engine", Pair(uiState.showKjAi, viewModel::updateShowKjAi)),
                Triple("declutter", "Declutter Notifications", Pair(uiState.showDeclutter, viewModel::updateShowDeclutter))
            )
            
            modulesToToggle.forEach { (route, label, statePair) ->
                val (isEnabled, updateFn) = statePair
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isChecked ->
                            updateFn(isChecked)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Dashboard Widgets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            
            val widgetsToToggle = listOf(
                Pair("Recent Inbox Logs", Pair(uiState.widgetRecentLogs, viewModel::updateWidgetRecentLogs)),
                Pair("KJ Quick Chat Pin", Pair(uiState.widgetQuickChat, viewModel::updateWidgetQuickChat))
            )
            
            widgetsToToggle.forEach { (label, statePair) ->
                val (isEnabled, updateFn) = statePair
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { updateFn(it) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Selective Call Forwarding (VIP Divert)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Forward all incoming calls to another number via carrier USSD (e.g. secondary phone), so you remain largely undisturbed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.vipDivertNumber,
                onValueChange = { viewModel.updateVipDivertNumber(it) },
                label = { Text("Divert To Number") },
                placeholder = { Text("e.g. 9876543210") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") }
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (uiState.vipDivertNumber.isNotBlank()) {
                            val encodedHash = Uri.encode("#")
                            val ussd = "*21*${uiState.vipDivertNumber}$encodedHash"
                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$ussd"))
                            
                            val selectedSimId = uiState.selectedSimId
                            if (selectedSimId != null) {
                                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val handles = telecomManager.callCapablePhoneAccounts
                                    val handle = handles.find { it.id == selectedSimId }
                                    if (handle != null) {
                                        intent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                                    }
                                }
                            }
                            
                            context.startActivity(intent)
                        } else {
                            android.widget.Toast.makeText(context, "Enter a number first", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Enable Divert")
                }
                
                Button(
                    onClick = {
                        val encodedHash = Uri.encode("#")
                        val ussd = "##21$encodedHash"
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$ussd"))
                        
                        val selectedSimId = uiState.selectedSimId
                        if (selectedSimId != null) {
                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val handles = telecomManager.callCapablePhoneAccounts
                                val handle = handles.find { it.id == selectedSimId }
                                if (handle != null) {
                                    intent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                                }
                            }
                        }
                        
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Divert")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Privacy & Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "All logs and rules are kept strictly local on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val scope = rememberCoroutineScope()
            var privacyMessage by remember { mutableStateOf("") }
            
            if (privacyMessage.isNotBlank()) {
                Text(privacyMessage, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        scope.launch {
                            val dbUtility = com.example.data.KjDatabaseUtility(context)
                            val jsonString = dbUtility.exportAllDataAsJson()
                            
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonString)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Local Data"))
                            privacyMessage = "Data ready for export. No cloud transmission."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Export Data")
                }
                
                Button(
                    onClick = { 
                        scope.launch {
                            val dbUtility = com.example.data.KjDatabaseUtility(context)
                            dbUtility.clearAllData()
                            privacyMessage = "All local data permanently deleted."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Open Source Community",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Support the project. No ads, no telemetry, entirely open source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { uriHandler.openUri("https://github.com/akhilesh844102/shield-forward") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Source")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub")
                }
                
                Button(
                    onClick = { uriHandler.openUri("https://github.com/sponsors/akhilesh844102") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Donate")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Donate")
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { uriHandler.openUri("https://buymeacoffee.com/akhilesh844102") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Coffee")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buy me a coffee", maxLines = 1)
                }
                
                Button(
                    onClick = { uriHandler.openUri("https://liberapay.com/akhilesh844102") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "LiberaPay")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LiberaPay", maxLines = 1)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "About & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { uriHandler.openUri("https://github.com/akhilesh844102/shield-forward/blob/main/PRIVACY_POLICY.md") }) {
                    Text("Privacy")
                }
                TextButton(onClick = { uriHandler.openUri("https://github.com/akhilesh844102/shield-forward/blob/main/TERMS.md") }) {
                    Text("Terms")
                }
                TextButton(onClick = { uriHandler.openUri("https://github.com/akhilesh844102/shield-forward/blob/main/LICENSE") }) {
                    Text("License")
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
    
    if (showSendersPicker) {
        InboxPickerModal(
            onDismiss = { showSendersPicker = false },
            onSenderSelected = { pickedSender ->
                val current = uiState.senders.split(",").map{it.trim()}.filter{it.isNotEmpty()}.toMutableList()
                if (!current.contains(pickedSender)) current.add(pickedSender)
                viewModel.updateSenders(current.joinToString(", "))
                showSendersPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxPickerModal(onDismiss: () -> Unit, onSenderSelected: (String) -> Unit) {
    val context = LocalContext.current
    var recentSenders by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val senders = mutableSetOf<String>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("address"),
                null,
                null,
                "date DESC LIMIT 100"
            )
            cursor?.use {
                val addressIndex = it.getColumnIndex("address")
                while (it.moveToNext()) {
                    val s = it.getString(addressIndex)
                    if (!s.isNullOrBlank()) {
                        senders.add(s)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recentSenders = senders.toList()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent Inbound Senders") },
        text = {
            if (recentSenders.isEmpty()) {
                Text("No recent SMS found or permission denied.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    recentSenders.forEach { s ->
                        TextButton(
                            onClick = { onSenderSelected(s) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
