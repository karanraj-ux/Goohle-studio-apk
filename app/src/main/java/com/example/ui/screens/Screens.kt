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
                text = "Simulate an incoming SMS and measure processing time without sending a real SMS or being connected to a network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Mock Sender") },
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
                        val result = SmsProcessor.processReceivedMessage(context, sender, message, isSimulation = true)
                        testResult = result.status
                        testDurationMs = result.durationMs
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
                        containerColor = if (testResult == "SUCCESS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Simulation Result: $testResult",
                            fontWeight = FontWeight.Bold,
                            color = if (testResult == "SUCCESS") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Processing Time: ${testDurationMs}ms\n(Measured inside background worker)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResult == "SUCCESS") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
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
    var senders by remember { mutableStateOf(prefs.getString("senders", "") ?: "") }
    var keywordFilter by remember { mutableStateOf(prefs.getString("keyword_filter", "") ?: "") }
    
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
                },
                label = { Text("Forward To Numbers (Comma Separated)") },
                placeholder = { Text("+123, +456") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) }
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
                    Icon(Icons.Default.List, contentDescription = "Pick Senders from Inbox", tint = MaterialTheme.colorScheme.primary)
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
