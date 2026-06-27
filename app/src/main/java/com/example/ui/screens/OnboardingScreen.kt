package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }

    val hasSmsPerms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
            
    val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    val hasNotificationPerm = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    val hasBatteryPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
    val hasCallScreeningRole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
        roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
    } else {
        true
    }

    val reqSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results -> 
        if (results.values.all { it }) step++ 
    }
    
    val reqCall = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results -> 
        if (results.values.all { it }) step++ 
    }

    val reqRole = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        step++
    }

    LaunchedEffect(step, hasSmsPerms, hasCallPerm, hasNotificationPerm, hasBatteryPerm, hasCallScreeningRole) {
        if (hasSmsPerms && hasCallPerm && hasNotificationPerm && hasBatteryPerm && hasCallScreeningRole) {
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
            "We need a few permissions to protect you from scams, track financial expenses securely, and manage autonomous calls.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        // Step 1: SMS
        PermissionCard(
            title = "SMS Access",
            description = "Needed to scan for scams and financial debits safely on-device.",
            icon = Icons.AutoMirrored.Filled.Message,
            isGranted = hasSmsPerms,
            onClick = {
                reqSms.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS))
            }
        )

        // Step 2: Phone Call
        PermissionCard(
            title = "Phone Calls",
            description = "Needed to automatically answer scam calls via KJ model or intercept known spam.",
            icon = Icons.Default.Call,
            isGranted = hasCallPerm,
            onClick = {
                reqCall.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG))
            }
        )

        // Step 3: Notification Listener
        PermissionCard(
            title = "Notification Interceptor",
            description = "Needed to block scam OTPs natively before they bother you.",
            icon = Icons.Default.Notifications,
            isGranted = hasNotificationPerm,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )

        // Step 4: Battery Optimization
        PermissionCard(
            title = "Battery Optimization",
            description = "Exclude Shield from battery sleeping so we never miss a Scam text.",
            icon = Icons.Default.BatteryFull,
            isGranted = hasBatteryPerm,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        )

        // Step 5: Call Screening Default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionCard(
                title = "Caller ID & Spam App",
                description = "Required to natively block calls and show warning screens on Android 10+.",
                icon = Icons.Default.Security,
                isGranted = hasCallScreeningRole,
                onClick = {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                    if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                        reqRole.launch(intent)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Text("Proceed to App", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if(isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = if(isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            } else {
                Button(
                    onClick = onClick,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
