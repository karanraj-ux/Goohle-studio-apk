package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainViewModel
import com.example.shield.ScheduledTaskWorker
import com.example.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: MainViewModel, onNavigateToAdd: () -> Unit) {
    val context = LocalContext.current
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.scheduledTaskRepository
        )
    )
    val tasks by scheduleViewModel.scheduledTasks.collectAsStateWithLifecycle(initialValue = emptyList())

    var type by remember { mutableStateOf("SMS") }
    var target by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSmsPicker by remember { mutableStateOf(false) }

    var showLinkDialogType by remember { mutableStateOf<String?>(null) }
    var linkUrlInput by remember { mutableStateOf("") }
    var linkLabelInput by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timePickerState = rememberTimePickerState()

    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }

    val dateStr = selectedDateMillis?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select Date"
    val timeStr = if (selectedHour != null && selectedMinute != null) {
        String.format("%02d:%02d", selectedHour, selectedMinute)
    } else {
        "Select Time"
    }

    val contactPicker = contactPickerLauncher { number -> target = number }
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showSmsPicker = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Scheduled Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Plan SMS, Calls or WhatsApp reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Task Form embedded
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Quick Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = type == "SMS",
                                onClick = { type = "SMS" },
                                label = { Text("SMS") },
                                leadingIcon = { if (type == "SMS") Icon(Icons.Default.Message, null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = type == "Call",
                                onClick = { type = "Call" },
                                label = { Text("Call") },
                                leadingIcon = { if (type == "Call") Icon(Icons.Default.Call, null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = type == "WhatsApp",
                                onClick = { type = "WhatsApp" },
                                label = { Text("WhatsApp") },
                                leadingIcon = { if (type == "WhatsApp") Icon(Icons.Default.ChatBubble, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = target,
                                onValueChange = { target = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { contactPicker() },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = "Pick Contact", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        
                        if (type != "Call") {
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Message") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showLinkDialogType = "Image"
                                        linkUrlInput = ""
                                        linkLabelInput = "Photo"
                                    },
                                    label = { Text("Image Link", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showLinkDialogType = "File"
                                        linkUrlInput = ""
                                        linkLabelInput = "Document"
                                    },
                                    label = { Text("Drive / File Link", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(dateStr, style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(timeStr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        FilledTonalButton(
                            onClick = {
                                if (target.isNotBlank() && selectedDateMillis != null && selectedHour != null) {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = selectedDateMillis!!
                                        set(Calendar.HOUR_OF_DAY, selectedHour!!)
                                        set(Calendar.MINUTE, selectedMinute!!)
                                        set(Calendar.SECOND, 0)
                                    }
                                    val timeMillis = cal.timeInMillis
                                    val msg = if (type != "Call") message else null
                                    
                                    // Schedule task logic here
                                    scheduleViewModel.addTask(type, target, msg, timeMillis) { id ->
                                        // Schedule WorkManager task
                                        val delay = timeMillis - System.currentTimeMillis()
                                        if (delay > 0) {
                                            val data = Data.Builder()
                                                .putInt("taskId", id.toInt())
                                                .putString("type", type)
                                                .putString("target", target)
                                                .putString("message", msg)
                                                .build()
                                                
                                            val request = OneTimeWorkRequestBuilder<ScheduledTaskWorker>()
                                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                                .setInputData(data)
                                                .build()
                                                
                                            WorkManager.getInstance(context).enqueue(request)
                                        }
                                    }
                                    
                                    // Reset form
                                    target = ""
                                    message = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = target.isNotBlank() && selectedDateMillis != null && selectedHour != null
                        ) {
                            Text("Schedule", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (tasks.isNotEmpty()) {
                    Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                    com.example.ui.screens.EmptyStateView("No scheduled tasks", "Tasks you schedule will appear here.", Icons.Rounded.Schedule)
                }
            }

            items(tasks, key = { it.id }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (task.type) {
                            "SMS" -> Icons.Default.Message
                            "Call" -> Icons.Default.Call
                            "Ghost Mode" -> Icons.Default.Lock
                            else -> Icons.Default.ChatBubble
                        }
                        Box(
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.target, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (task.type != "Call") {
                                Text(task.message ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (task.message?.contains("http://") == true || task.message?.contains("https://") == true) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Media / File Link Attached",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(task.timeMillis)) + if (task.isRecurring) " (Recurring)" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (task.completed) {
                            Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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
                                "Why use Pause Automation?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        BenefitRow(
                            icon = Icons.Rounded.CloudOff,
                            title = "Local Execution",
                            description = "Your scheduled messages and calls are handled entirely on your device."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BenefitRow(
                            icon = Icons.Rounded.Lock,
                            title = "Privacy First",
                            description = "No AI analysis of your messages. No external server syncing."
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    if (showLinkDialogType != null) {
        AlertDialog(
            onDismissRequest = { showLinkDialogType = null },
            icon = {
                Icon(
                    if (showLinkDialogType == "Image") Icons.Default.Image else Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Attach ${showLinkDialogType} Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Paste a viewable web URL (e.g. Google Drive, Dropbox, public image URL, or document link) to include in your scheduled SMS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
                        label = { Text("URL / Web Link") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = linkLabelInput,
                        onValueChange = { linkLabelInput = it },
                        label = { Text("Label (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedUrl = linkUrlInput.trim()
                        if (trimmedUrl.isNotEmpty()) {
                            val validUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) "https://$trimmedUrl" else trimmedUrl
                            val tag = if (linkLabelInput.isNotBlank()) linkLabelInput.trim() else showLinkDialogType
                            val linkStr = "\n[$tag: $validUrl]"
                            message = (message + linkStr).trimStart()
                        }
                        showLinkDialogType = null
                    },
                    enabled = linkUrlInput.isNotBlank()
                ) {
                    Text("Attach Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialogType = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
