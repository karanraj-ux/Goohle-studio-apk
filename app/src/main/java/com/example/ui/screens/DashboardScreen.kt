package com.example.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.MainViewModel
import com.example.Screen
import com.example.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel, 
    navController: NavHostController, 
    windowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }
    var showContactPermissionRationale by remember { mutableStateOf(false) }

    val notificationPolicyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            settingsViewModel.updateOverrideDnd(true)
            val policy = android.app.NotificationManager.Policy(
                android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED
            )
            nm.notificationPolicy = policy
        }
    }
    
    // Also apply it periodically if enabled
    LaunchedEffect(settingsState.overrideDnd) {
        if (settingsState.overrideDnd) {
            val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                val policy = android.app.NotificationManager.Policy(
                    android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                    android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                    android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                )
                nm.notificationPolicy = policy
            }
        }
    }
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settingsViewModel.updateDndBypassRingtoneUri(uri.toString())
            }
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    
                    if (hasPhoneIndex >= 0 && idIndex >= 0) {
                        val hasPhone = cursor.getInt(hasPhoneIndex)
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                        
                        if (hasPhone > 0) {
                            val id = cursor.getString(idIndex)
                            val phones = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
                                null, 
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", 
                                arrayOf(id), 
                                null
                            )
                            if (phones != null && phones.moveToFirst()) {
                                val numIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIndex >= 0) {
                                    val currentVips = settingsState.vipCallers
                                    val numStr = phones.getString(numIndex)
                                    val newVips = if (currentVips.isEmpty()) "$name ($numStr)" else "$currentVips,$name ($numStr)"
                                    settingsViewModel.updateVipCallers(newVips)
                                    
                                    // Also set the STARRED status in the Android Contacts Database
                                    try {
                                        val values = android.content.ContentValues()
                                        values.put(android.provider.ContactsContract.Contacts.STARRED, 1)
                                        context.contentResolver.update(
                                            android.provider.ContactsContract.Contacts.CONTENT_URI,
                                            values,
                                            android.provider.ContactsContract.Contacts._ID + " = ?",
                                            arrayOf(id)
                                        )
                                        scope.launch { snackbarHostState.showSnackbar("VIP Saved & Starred to bypass DND!") }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        scope.launch { snackbarHostState.showSnackbar("Added VIP, but could not star contact.") }
                                    }
                                }
                                phones.close()
                            }
                        } else {
                            val currentVips = settingsState.vipCallers
                            val newVips = if (currentVips.isEmpty()) name else "$currentVips,$name"
                            settingsViewModel.updateVipCallers(newVips)
                            scope.launch { snackbarHostState.showSnackbar("Important Contact Saved Successfully!") }
                        }
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results[android.Manifest.permission.READ_CONTACTS] == true) {
            contactPickerLauncher.launch(null)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showContactPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showContactPermissionRationale = false },
            title = { Text("Permission Required") },
            text = { Text("We need permission to read and write your contacts to automatically mark VIPs as 'Starred' so they bypass the Do Not Disturb policy.") },
            confirmButton = {
                TextButton(onClick = {
                    showContactPermissionRationale = false
                    contactPermissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS))
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Battery Optimization Banner
            if (!isIgnoringBatteryOptimizations) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clickable {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Battery Optimization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Mina Assistant needs to run in the background to reliably block calls, forward SMS, and execute scheduled tasks. Tap to allow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // 1. Top Section: Assistant Hook & Privacy Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Hi, I'm Mina",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline • No tracking • Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 2. Activity Overview / Reports
            item {
                Text(
                    text = "Activity Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (windowSizeClass == WindowWidthSizeClass.Expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = "Spam Blocked", value = settingsState.spamBlockedCount.toString(), icon = Icons.Rounded.Shield,
                            color = androidx.compose.ui.graphics.Color(0xFFFF7043), modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "SMS Forwarded", value = mainUiState.totalForwarded.toString(), icon = Icons.Rounded.ForwardToInbox,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Tasks Today", value = mainUiState.tasksToday.toString(), icon = Icons.Rounded.Schedule,
                            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Expenses", value = "$120", icon = Icons.Rounded.AccountBalanceWallet,
                            color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Spam Blocked", value = settingsState.spamBlockedCount.toString(), icon = Icons.Rounded.Shield,
                            color = androidx.compose.ui.graphics.Color(0xFFFF7043), modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "SMS Forwarded", value = mainUiState.totalForwarded.toString(), icon = Icons.Rounded.ForwardToInbox,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Tasks Today", value = mainUiState.tasksToday.toString(), icon = Icons.Rounded.Schedule,
                            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item { GhostModeCard(settingsState, settingsViewModel, snackbarHostState) }
            // 3. Smart DND Section (Structured, No big toggle)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Smart Silent Mode", 
                                     style = MaterialTheme.typography.titleMedium, 
                                     fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = settingsState.overrideDnd,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        if (!nm.isNotificationPolicyAccessGranted) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            try {
                                                notificationPolicyLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            settingsViewModel.updateOverrideDnd(true)
                                        }
                                    } else {
                                        settingsViewModel.updateOverrideDnd(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Silences all incoming calls except your important contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // VIPs List
                        val vips = settingsState.vipCallers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        Text(
                            "Important Contacts (Always Ring)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (vips.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                vips.forEach { vip ->
                                    InputChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(vip) },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Contact",
                                                modifier = Modifier.size(16.dp).clickable {
                                                    val newList = vips.filter { it != vip }.joinToString(",")
                                                    settingsViewModel.updateVipCallers(newList)
                                                }
                                            )
                                        },
                                        border = null,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No important contacts added yet. All calls will be silenced.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    showContactPermissionRationale = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add VIP Number (Bypasses All)")
                        }
                    }
                }
            }
            
            // Advanced settings section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .clickable { showAdvanced = !showAdvanced },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Persistent Caller Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(
                                if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    "Allow unknown numbers to bypass silent mode if they call multiple times in a row.", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Bypass after", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    var expandedCalls by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable {
                                            expandedCalls = true
                                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("${settingsState.dndThresholdCalls} calls", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        DropdownMenu(expanded = expandedCalls, onDismissRequest = { expandedCalls = false }) {
                                            listOf(2, 3, 4, 5).forEach { callCount ->
                                                DropdownMenuItem(
                                                    text = { Text("$callCount calls") },
                                                    onClick = {
                                                        settingsViewModel.updateDndThresholdCalls(callCount.toString())
                                                        expandedCalls = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Within timeframe of", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    var expandedMins by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable {
                                            expandedMins = true
                                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("${settingsState.dndTimeframeMinutes} mins", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        DropdownMenu(expanded = expandedMins, onDismissRequest = { expandedMins = false }) {
                                            listOf(1, 2, 3, 5, 10, 15).forEach { mins ->
                                                DropdownMenuItem(
                                                    text = { Text("$mins mins") },
                                                    onClick = {
                                                        settingsViewModel.updateDndTimeframeMinutes(mins.toString())
                                                        expandedMins = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (mainUiState.recentLogs.isNotEmpty()) {
                item {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(mainUiState.recentLogs.take(5)) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when(log.status) {
                                "SPAM_BLOCKED" -> Icons.Rounded.Shield
                                "CALL_FORWARDED" -> Icons.Rounded.PhoneForwarded
                                "SUCCESS" -> Icons.Rounded.ForwardToInbox
                                else -> Icons.Rounded.History
                            }
                            val color = when(log.status) {
                                "SPAM_BLOCKED" -> Color(0xFFFF7043)
                                "CALL_FORWARDED" -> MaterialTheme.colorScheme.primary
                                "SUCCESS" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = color)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("From: ${log.sender}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Premium Explanation Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Why use Mina Assistant?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        BenefitRow(
                            icon = Icons.Rounded.Shield,
                            title = "Strictly On-Device Processing",
                            description = "Your call logs, messages, and contacts never leave your phone."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BenefitRow(
                            icon = Icons.Rounded.CloudOff,
                            title = "No Server, No AI Agent",
                            description = "We don't use AI or external servers to read your data. Logic is hardcoded safely on your device."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BenefitRow(
                            icon = Icons.Rounded.Favorite,
                            title = "Human-Centric Routing",
                            description = "Smart rules like 'Persistent Caller' allow real emergencies to bypass silent mode naturally."
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .semantics { contentDescription = "$title is $value" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
