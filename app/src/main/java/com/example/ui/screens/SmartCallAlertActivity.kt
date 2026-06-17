package com.example.ui.screens

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class SmartCallAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        val contactName = intent.getStringExtra("CONTACT_NAME") ?: "Unknown Contact"
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        val message = intent.getStringExtra("MESSAGE") ?: "Number is now available"

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    CallAlertContent(
                        contactName = contactName,
                        phoneNumber = phoneNumber,
                        message = message,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun CallAlertContent(
    contactName: String,
    phoneNumber: String,
    message: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = contactName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        if (phoneNumber.isNotBlank()) {
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Dismiss
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Ignore", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ignore", style = MaterialTheme.typography.labelMedium)
            }
            
            // Schedule
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(AlarmClock.EXTRA_MESSAGE, "Call $contactName")
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        }
                        if (alarmIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(alarmIntent)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Schedule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Schedule", style = MaterialTheme.typography.labelMedium)
            }
            
            // 3x Call
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                        scope.launch {
                            val db = com.example.data.AppDatabase.getDatabase(context)
                            val job = com.example.data.CallJobEntity(
                                phoneNumber = phoneNumber,
                                totalCalls = 3,
                                callsMade = 1,
                                intervalMinutes = 2,
                                nextCallTime = System.currentTimeMillis() + (2 * 60 * 1000), // Next call in 2 mins
                                description = "Triggered from Smart Alert"
                            )
                            val id = db.callJobDao().insert(job)
                            val savedJob = job.copy(id = id)
                            com.example.calls.CallManager.scheduleNextCall(context, savedJob)
                            
                            // Make first call now
                            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(callIntent)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = "3x Call", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("3x Call", style = MaterialTheme.typography.labelMedium)
            }

            // Tap to Call
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                        context.startActivity(callIntent)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Call Now", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
