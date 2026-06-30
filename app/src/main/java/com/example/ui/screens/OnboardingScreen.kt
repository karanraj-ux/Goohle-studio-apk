package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    
    // Check Current Permissions
    val hasSmsPerms = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
            
    val hasCallPerm = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }

    val hasNotificationPerm = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Step state: 0=SMS, 1=CallLog, 2=Notifications, 3=Done
    var currentStep by remember { mutableStateOf(0) }

    // Launchers
    val reqSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results -> 
        val granted = results.values.all { it }
        hasSmsPerms.value = granted
        if (granted) {
            currentStep = 1
        } else {
            Toast.makeText(context, "SMS permissions denied. Basic forwarding will not work.", Toast.LENGTH_LONG).show()
        }
    }
    
    val reqCall = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results -> 
        val granted = results.values.all { it }
        hasCallPerm.value = granted
        if (granted) {
            currentStep = 2
        } else {
            Toast.makeText(context, "Call log permissions denied.", Toast.LENGTH_LONG).show()
        }
    }

    val reqNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> 
        hasNotificationPerm.value = granted
        if (granted) {
            currentStep = 3
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-advance if already granted
    LaunchedEffect(currentStep, hasSmsPerms.value, hasCallPerm.value, hasNotificationPerm.value) {
        if (currentStep == 0 && hasSmsPerms.value) currentStep = 1
        if (currentStep == 1 && hasCallPerm.value) currentStep = 2
        if (currentStep == 2 && hasNotificationPerm.value) currentStep = 3
        
        if (currentStep >= 3) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Security",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Welcome to Shield", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Let's get set up. We'll ask for necessary permissions step-by-step so you control what we access.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        if (currentStep == 0) {
            PermissionCard(
                title = "Step 1: SMS Access",
                description = "Required to read incoming messages so they can be securely forwarded or analyzed locally.",
                icon = Icons.AutoMirrored.Filled.Message,
                isGranted = hasSmsPerms.value,
                onGrantClick = {
                    reqSms.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS))
                },
                onSkipClick = { currentStep = 1 }
            )
        } else if (currentStep == 1) {
            PermissionCard(
                title = "Step 2: Phone State & Call Log",
                description = "Required to identify incoming callers and log missed calls for your records.",
                icon = Icons.Default.Call,
                isGranted = hasCallPerm.value,
                onGrantClick = {
                    reqCall.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG))
                },
                onSkipClick = { currentStep = 2 }
            )
        } else if (currentStep == 2) {
            PermissionCard(
                title = "Step 3: Notifications",
                description = "Required to keep the background service alive and notify you when an action is taken.",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPerm.value,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        reqNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        currentStep = 3
                    }
                },
                onSkipClick = { currentStep = 3 }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        if (currentStep >= 3) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ) {
                Text("Proceed to App", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkipClick) {
                    Text("Skip for now")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onGrantClick) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
