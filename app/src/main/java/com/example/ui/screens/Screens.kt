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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.SmsProcessor
import kotlinx.coroutines.launch

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
                            com.example.shield.ShieldNotificationService.simulateNotificationProcessing(context, sender, message)
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
                Icon(Icons.Default.PlayArrow, contentDescription = null)
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
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE) }
    
    var targetNumbers by remember { mutableStateOf(prefs.getString("target_numbers", "") ?: "") }
    var webhookUrl by remember { mutableStateOf(prefs.getString("webhook_url", "") ?: "") }
    var senders by remember { mutableStateOf(prefs.getString("senders", "") ?: "") }
    var keywordFilter by remember { mutableStateOf(prefs.getString("keyword_filter", "") ?: "") }
    var vipDivertNumber by remember { mutableStateOf(prefs.getString("vip_divert_number", "") ?: "") }
    
    var showSendersPicker by remember { mutableStateOf(false) }
    
    // Auto-migrate legacy settings if target_numbers or senders is empty but legacy key is populated
    LaunchedEffect(Unit) {
        if (targetNumbers.isBlank()) {
            val legacy = prefs.getString("target_number", "") ?: ""
            if (legacy.isNotBlank()) {
                targetNumbers = legacy
                prefs.edit().putString("target_numbers", legacy).apply()
            }
        }
        if (senders.isBlank()) {
            val legacy = prefs.getString("sender_filter", "") ?: ""
            if (legacy.isNotBlank()) {
                senders = legacy
                prefs.edit().putString("senders", legacy).apply()
            }
        }
    }
    
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
                value = targetNumbers,
                onValueChange = { 
                    targetNumbers = it
                    prefs.edit().putString("target_numbers", it).apply()
                    // Update main app_prefs for ShieldNotificationService compatibility
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit().putString("forward_phone", it.split(",").firstOrNull()?.trim() ?: "").apply()
                },
                label = { Text("Forward To Numbers (SMS)") },
                placeholder = { Text("+123, +456") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )

            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { 
                    webhookUrl = it
                    prefs.edit().putString("webhook_url", it).apply()
                    // Update main app_prefs for ShieldNotificationService compatibility
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit().putString("webhook_url", it).apply()
                },
                label = { Text("Forward via Webhook URL (JSON)") },
                placeholder = { Text("https://my-server.com/webhook") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, null) }
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
                    value = senders,
                    onValueChange = { 
                        senders = it
                        prefs.edit().putString("senders", it).apply()
                    },
                    label = { Text("Match Senders (Comma Separated)") },
                    placeholder = { Text("Airtel, VK-HDFC") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                
                IconButton(onClick = { showSendersPicker = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Pick Senders from Inbox", tint = MaterialTheme.colorScheme.primary)
                }
            }

            OutlinedTextField(
                value = keywordFilter,
                onValueChange = { 
                    keywordFilter = it
                    prefs.edit().putString("keyword_filter", it).apply()
                },
                label = { Text("Match Keyword") },
                placeholder = { Text("e.g. OTP, Mitra") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            
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
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disable Battery Restrictions")
            }
            Text(
                text = "Important to keep the app working reliably in the background without high battery drain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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

            val appUiPrefs = remember { context.getSharedPreferences("app_ui_prefs", Context.MODE_PRIVATE) }
            val mainPrefs = remember { context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE) }
            
            Text("Navigation Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            val modulesToToggle = listOf(
                "kj_companion" to "KJ Chat Companion",
                "kj_ai" to "Local Engine",
                "calls" to "Auto Call handling",
                "shield" to "Shield Protection",
                "declutter" to "Declutter Notifications"
            )
            
            modulesToToggle.forEach { (route, label) ->
                var isEnabled by remember { mutableStateOf(appUiPrefs.getBoolean("show_$route", true)) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            appUiPrefs.edit().putBoolean("show_$route", it).apply()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Dashboard Widgets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            
            val widgetsToToggle = listOf(
                "widget_recent_logs" to "Recent Inbox Logs",
                "widget_quick_chat" to "KJ Quick Chat Pin"
            )
            
            widgetsToToggle.forEach { (prefKey, label) ->
                var isEnabled by remember { mutableStateOf(mainPrefs.getBoolean(prefKey, true)) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            mainPrefs.edit().putBoolean(prefKey, it).apply()
                        }
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
                value = vipDivertNumber,
                onValueChange = { 
                    vipDivertNumber = it
                    prefs.edit().putString("vip_divert_number", it).apply()
                },
                label = { Text("Divert To Number") },
                placeholder = { Text("e.g. 9876543210") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (vipDivertNumber.isNotBlank()) {
                            val encodedHash = Uri.encode("#")
                            val ussd = "*21*$vipDivertNumber$encodedHash"
                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$ussd"))
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
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Divert")
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
    
    if (showSendersPicker) {
        InboxPickerModal(
            onDismiss = { showSendersPicker = false },
            onSenderSelected = { pickedSender ->
                val current = senders.split(",").map{it.trim()}.filter{it.isNotEmpty()}.toMutableList()
                if (!current.contains(pickedSender)) current.add(pickedSender)
                senders = current.joinToString(", ")
                prefs.edit().putString("senders", senders).apply()
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
