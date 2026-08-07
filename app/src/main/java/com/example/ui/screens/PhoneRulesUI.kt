package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PhoneRuleEntity

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.ui.viewmodels.PhoneRuleViewModel

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color

@Composable
fun PhoneRulesUI(ruleViewModel: PhoneRuleViewModel) {
    val rules by ruleViewModel.rules.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rule Numbers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Text("Add Number")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (rules.isEmpty()) {
            Text("No rules configured. Add a number to set as Important Contact or Forward to Secondary Phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            rules.forEach { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.phoneNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val badges = mutableListOf<String>()
                            if (rule.isVip) badges.add("Important Contact (Bypass DND)")
                            if (rule.isDivert) badges.add("Forward to Secondary Phone (Auto-Reject)")
                            if (rule.isForward) badges.add("Forward (SMS Notification)")
                            Text(badges.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { ruleViewModel.removeRule(rule) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        var isVip by remember { mutableStateOf(false) }
        var isDivert by remember { mutableStateOf(false) }
        var isForward by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val contactPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickContact()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val cursor = context.contentResolver.query(
                            uri,
                            arrayOf(ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts._ID),
                            null,
                            null,
                            null
                        )
                        if (cursor != null && cursor.moveToFirst()) {
                            val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            val hasPhone = if (hasPhoneIndex >= 0) cursor.getString(hasPhoneIndex) else "0"
                            val id = if (idIndex >= 0) cursor.getString(idIndex) else ""
                            cursor.close()
                            
                            if (hasPhone == "1" && id.isNotEmpty()) {
                                val phones = context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id),
                                    null
                                )
                                if (phones != null && phones.moveToFirst()) {
                                    val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIndex >= 0) {
                                        val number = phones.getString(numberIndex)
                                        withContext(Dispatchers.Main) {
                                            phoneNumber = number ?: ""
                                        }
                                    }
                                    phones.close()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Configure Call Routing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "How should Mina handle calls from this number?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            }) {
                                Icon(Icons.Default.Contacts, contentDescription = "Pick Contact")
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Routing Action:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isVip) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { isVip = !isVip }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isVip, onCheckedChange = { isVip = it })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Important Contact Caller", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Always rings, even in Ghost Mode or DND.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDivert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { isDivert = !isDivert }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isDivert, onCheckedChange = { isDivert = it })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Forward to Secondary Phone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Instantly forward this caller to your secondary phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isForward) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { isForward = !isForward }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isForward, onCheckedChange = { isForward = it })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Smart Forward", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Trigger auto-reply if you miss their call.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val snackbarHostState = com.example.LocalSnackbarHostState.current
                val scope = rememberCoroutineScope()
                FilledTonalButton(onClick = {
                    if (phoneNumber.isNotBlank()) {
                        ruleViewModel.addRule(phoneNumber, isVip, isDivert, isForward)
                        scope.launch { snackbarHostState.showSnackbar("Rule Saved Successfully!") }
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
