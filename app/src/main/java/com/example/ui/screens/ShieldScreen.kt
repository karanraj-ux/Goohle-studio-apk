package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.SettingsViewModel
import com.example.ShieldApplication

@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    var isShieldActive by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    val settingsRepository = (context.applicationContext as ShieldApplication).container.settingsRepository
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(settingsRepository))
    val settingsState by settingsViewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isShieldActive = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
                )
                Text(
                    "Shield Status",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            
            if (!isIgnoringBatteryOptimizations) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Battery Warning", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Battery Restrictions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your phone's battery saver might kill the Shield. Tap below to prevent unexpected interruptions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bypass Battery Saver")
                        }
                    }
                }
            }

            val shieldContainerColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isShieldActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                animationSpec = androidx.compose.animation.core.tween(500),
                label = "shieldColor"
            )
            val shieldIconBgColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isShieldActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                animationSpec = androidx.compose.animation.core.tween(500),
                label = "shieldIconBg"
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = shieldContainerColor)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(shieldIconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isShieldActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (isShieldActive) "Shield Active" else "Shield Inactive",
                            tint = if (isShieldActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isShieldActive) "Active & Protecting" else "Shield is Offline",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isShieldActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isShieldActive) 
                                "Your device is protected against known text scams and shoulder surfing for OTPs." 
                               else 
                                "Enable Notification Access to allow Shield to intercept scams and secure OTPs.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isShieldActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )

                    if (!isShieldActive) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Enable Service")
                        }
                    }
                }
            }

            Text(
                "Protection Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
            )
            
            val masterKillSwitchColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (settingsState.masterKillSwitch) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "masterKillSwitchColor"
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = masterKillSwitchColor)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master Kill Switch", fontWeight = FontWeight.Bold, color = if (settingsState.masterKillSwitch) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Disables ALL background interception, forwarding, and rules.", style = MaterialTheme.typography.bodySmall, color = if (settingsState.masterKillSwitch) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settingsState.masterKillSwitch,
                        onCheckedChange = { settingsViewModel.updateMasterKillSwitch(it) }
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hidden", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Silent Swallow", fontWeight = FontWeight.SemiBold)
                        Text("Drop scam notifications silently without vibrating.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settingsState.silentSwallow,
                        onCheckedChange = { settingsViewModel.updateSilentSwallow(it) }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Block, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scam Blocking", fontWeight = FontWeight.SemiBold)
                        Text("Blocks notifications containing known phishing markers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

