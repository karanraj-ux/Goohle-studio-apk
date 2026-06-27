package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.CallsViewModel
import com.example.calls.CallManager
import com.example.data.CallJobEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: CallsViewModel = viewModel(
        factory = CallsViewModel.Factory(
            (LocalContext.current.applicationContext as com.example.ShieldApplication).container.callJobRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val jobs = uiState.activeCallJobs
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCallPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Call permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Notification permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Ask for notif permission if needed
    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            Toast.makeText(context, "Exact alarm permission required", Toast.LENGTH_SHORT).show()
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            return@FloatingActionButton
                        }
                    }
                    
                    if (hasCallPermission) {
                        showDialog = true 
                    } else {
                        permissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Scheduled Call")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 20.dp)) {
            
            Text(
                "Calls & Redial", 
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Call, contentDescription = "Active call rules", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No active call rules",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the + button to schedule calls",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(jobs) { job ->
                        CallJobCard(
                            job = job, 
                            onDelete = { 
                                CallManager.cancelJobAalrm(context, job.id)
                                viewModel.deleteCallJob(job.id) 
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddCallJobDialog(
            onDismiss = { showDialog = false },
            onSave = { phone, calls, interval, timeMs, desc ->
                val newJob = CallJobEntity(
                    phoneNumber = phone,
                    totalCalls = calls,
                    intervalMinutes = interval,
                    nextCallTime = timeMs,
                    description = desc
                )
                viewModel.insertCallJob(newJob) { newId ->
                    CallManager.scheduleNextCall(context, newJob.copy(id = newId))
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun CallJobCard(job: CallJobEntity, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val nextTime = remember(job.nextCallTime) { formatter.format(Date(job.nextCallTime)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(job.phoneNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                if (job.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(job.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Calls: ${job.callsMade}/${job.totalCalls} • Interval: ${job.intervalMinutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Next: $nextTime", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (job.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (job.isActive) "Active" else "Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (job.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddCallJobDialog(onDismiss: () -> Unit, onSave: (String, Int, Int, Long, String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalCalls by remember { mutableStateOf("1") }
    var interval by remember { mutableStateOf("0") }
    var delayMinutes by remember { mutableStateOf("0") }
    
    val isRedial = totalCalls.toIntOrNull() ?: 1 > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Auto Call") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = delayMinutes,
                        onValueChange = { delayMinutes = it },
                        label = { Text("Delay (min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalCalls,
                        onValueChange = { totalCalls = it },
                        label = { Text("Total Calls") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                if (isRedial) {
                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it },
                        label = { Text("Interval (min)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = phone.trim()
                if (p.isNotEmpty()) {
                    val c = totalCalls.toIntOrNull() ?: 1
                    val i = interval.toIntOrNull() ?: 0
                    val d = delayMinutes.toIntOrNull() ?: 0
                    val timeMs = System.currentTimeMillis() + (d * 60 * 1000L)
                    onSave(p, c, i, timeMs, description)
                }
            }) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
