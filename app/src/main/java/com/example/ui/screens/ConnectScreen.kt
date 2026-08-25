package com.example.ui.screens

import kotlinx.coroutines.launch


import android.Manifest
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainViewModel
import com.example.ui.viewmodels.SettingsViewModel


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Auto-Reply", fontWeight = FontWeight.Bold) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Forwarding", fontWeight = FontWeight.Bold) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (selectedTab == 0) {
            AutoReplyTab(viewModel)
        } else {
            ForwardingTab(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoReplyTab(viewModel: MainViewModel) {

    val context = LocalContext.current
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] == true
        val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] == true
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true
        
        if (smsGranted && callLogGranted && phoneStateGranted) {
            settingsViewModel.updateAutoRespondMissedCall(true)
        } else {
            settingsViewModel.updateAutoRespondMissedCall(false)
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
                                    val number = phones.getString(numIndex)
                                    // Just storing name for display, ideally we'd store name & number
                                    val currentList = settingsState.autoReplyRestrictedNumbers
                                    val newList = if (currentList.isEmpty()) name else "$currentList,$name"
                                    settingsViewModel.updateAutoReplyRestrictedNumbers(newList)
                                }
                                phones.close()
                            }
                        } else {
                            val currentList = settingsState.autoReplyRestrictedNumbers
                            val newList = if (currentList.isEmpty()) name else "$currentList,$name"
                            settingsViewModel.updateAutoReplyRestrictedNumbers(newList)
                        }
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Top Section: Switch and Title
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Quickreply, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Missed Calls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = settingsState.autoRespondMissedCall,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.SEND_SMS,
                                            Manifest.permission.READ_CALL_LOG,
                                            Manifest.permission.READ_PHONE_STATE
                                        )
                                    )
                                } else {
                                    settingsViewModel.updateAutoRespondMissedCall(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Incoming SMS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = settingsState.autoRespondSms,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.SEND_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                    settingsViewModel.updateAutoRespondSms(true)
                                } else {
                                    settingsViewModel.updateAutoRespondSms(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (!settingsState.autoRespondMissedCall && !settingsState.autoRespondSms) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "When active, Mina will politely text people who try to reach you while you're busy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 2. Chat UI for Custom Message
        item {
            AnimatedVisibility(
                visible = settingsState.autoRespondMissedCall,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Chat Mock UI
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Incoming (Missed Call)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PhoneMissed, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Missed Call", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Outgoing (Auto Reply)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mina Auto-Reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = settingsState.busyReplyMessage,
                                            onValueChange = { settingsViewModel.updateBusyReplyMessage(it) },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimary),
                                            decorationBox = { innerTextField ->
                                                if (settingsState.busyReplyMessage.isEmpty()) {
                                                    Text("Type your auto-reply here...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Relationship Tiers Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth()
                        ) {
                            Text("Who gets Auto-Replies?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Auto-Responder integrates directly with your Relationship Tiers. You don't need to manually pick people.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ListItem(
                                headlineContent = { Text("Inner Circle (VIPs)") },
                                supportingContent = { Text("Will receive auto-replies if you miss their call") },
                                leadingContent = { Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            ListItem(
                                headlineContent = { Text("Standard Contacts") },
                                supportingContent = { Text("Will receive auto-replies") },
                                leadingContent = { Icon(Icons.Rounded.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                            )
                            ListItem(
                                headlineContent = { Text("Muted / Blocked") },
                                supportingContent = { Text("Will NEVER receive auto-replies (silent rejection)") },
                                leadingContent = { Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }
        
        // Custom Reply Message
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Auto-Reply Message",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This message will be sent to callers or texters when your shield is active or you miss their call.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = settingsState.busyReplyMessage,
                        onValueChange = { settingsViewModel.updateBusyReplyMessage(it) },
                        label = { Text("Your custom reply message") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        
        // 3. Premium Explanation Card
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
                            "Why use Auto-Responder?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BenefitRow(
                        icon = Icons.Rounded.Favorite,
                        title = "Polite Professionalism",
                        description = "Never leave someone hanging. Automatically acknowledge important calls when you're busy."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "100% On-Device",
                        description = "Your contacts and messages are never sent to a cloud server."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.SettingsSuggest,
                        title = "Smart Context",
                        description = "Only replies to actual missed calls, not spam or blocked numbers."
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForwardingTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    val ruleViewModel: com.example.ui.viewmodels.PhoneRuleViewModel = viewModel(
        factory = com.example.ui.viewmodels.PhoneRuleViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.phoneRuleRepository
        )
    )
    val rules by ruleViewModel.rules.collectAsStateWithLifecycle(initialValue = emptyList())
    var showInboxPicker by remember { mutableStateOf(false) }

    


    val targetPhonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    
                    if (hasPhoneIndex >= 0 && idIndex >= 0) {
                        val hasPhone = cursor.getInt(hasPhoneIndex)
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
                                    val number = phones.getString(numIndex)
                                    settingsViewModel.updateSmsForwardTarget(number ?: "")
                                }
                                phones.close()
                            }
                        }
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

            val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true && permissions[Manifest.permission.ANSWER_PHONE_CALLS] == true) {
            settingsViewModel.updateAutoForwardCalls(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Call permissions are required to forward calls") }
        }
    }


        val smsForwardPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val sendGranted = results[Manifest.permission.SEND_SMS] == true
        val receiveGranted = results[Manifest.permission.RECEIVE_SMS] == true
        val readGranted = results[Manifest.permission.READ_SMS] == true
        if (sendGranted && receiveGranted && readGranted) {
            settingsViewModel.updateSmsForwardingEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("SMS Send, Receive, and Read permissions are required") }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            targetPhonePickerLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Contacts permission is required to pick a number") }
        }
    }
    

    
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            showInboxPicker = true
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        
        // Call Forwarding Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PhoneForwarded, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Call Forwarding Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                            OutlinedTextField(
                                value = settingsState.forwardPhone,
                                onValueChange = { settingsViewModel.updateForwardPhone(it) },
                                label = { Text("Secondary Phone (Call Forwarding Target)") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        if (!settingsState.autoForwardCalls) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(true)
                                            }
                                        } else {
                                            settingsViewModel.updateAutoForwardCalls(false)
                                        }
                                    }.padding(16.dp).fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Forward Unknown Calls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        Text("Forwards callers not in Important Contacts to your secondary phone and auto-replies to them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = settingsState.autoForwardCalls,
                                                                                                                        onCheckedChange = { isChecked -> 
                                            if (isChecked) {
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                                } else {
                                                    settingsViewModel.updateAutoForwardCalls(true)
                                                }
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(false)
                                            }
                                        }
                                    )
                                }
                            }

                            PhoneRulesUI(ruleViewModel)
                        }
                }
            }
        }
        
        // Contextual Integrations Card
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
                            "Contextual Integrations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BenefitRow(
                        icon = Icons.Rounded.Event,
                        title = "Calendar Sync (Ghost Mode)",
                        description = "Automatically block all non-VIP calls when you have a busy event on your calendar."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.Videocam,
                        title = "Deep Work Detection",
                        description = "Instantly mute calls when you are in a Zoom, Teams, or Meet video conference."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.DirectionsCar,
                        title = "Drive Mode",
                        description = "Activate automatic WhatsApp redirect replies when connected to your car's Bluetooth."
                    )
                }
            }
        }
    }
}
