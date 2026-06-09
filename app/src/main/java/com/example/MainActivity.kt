package com.example

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmsForwarderApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SmsForwarderApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE) }
    
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("is_enabled", false)) }
    var targetNumber by remember { mutableStateOf(prefs.getString("target_number", "") ?: "") }
    var senderFilter by remember { mutableStateOf(prefs.getString("sender_filter", "") ?: "") }
    var keywordFilter by remember { mutableStateOf(prefs.getString("keyword_filter", "") ?: "") }
    
    var permissionsGranted by remember { mutableStateOf(true) }

    val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,   
        Manifest.permission.READ_PHONE_STATE
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted && isEnabled) {
            isEnabled = false
            prefs.edit().putBoolean("is_enabled", false).apply()
            Toast.makeText(context, "Permissions required to enable auto-forwarding", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Auto SMS Forwarder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Forwarding",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnabled) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !permissionsGranted) {
                            permissionLauncher.launch(permissions)
                        } else {
                            isEnabled = checked
                            prefs.edit().putBoolean("is_enabled", checked).apply()
                        }
                    }
                )
            }
        }

        if (!permissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "SMS Permissions are not granted. Please allow permissions for the app to function.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        OutlinedTextField(
            value = targetNumber,
            onValueChange = { 
                targetNumber = it
                prefs.edit().putString("target_number", it).apply()
            },
            label = { Text("Forward To Number (Required)") },
            placeholder = { Text("e.g. +1234567890") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "If filters are empty, ALL messages will be forwarded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = senderFilter,
            onValueChange = { 
                senderFilter = it
                prefs.edit().putString("sender_filter", it).apply()
            },
            label = { Text("Match Sender (Optional)") },
            placeholder = { Text("e.g. Airtel, Bank, +1234") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = keywordFilter,
            onValueChange = { 
                keywordFilter = it
                prefs.edit().putString("keyword_filter", it).apply()
            },
            label = { Text("Match Keyword (Optional)") },
            placeholder = { Text("e.g. OTP, Mitra, Login") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
