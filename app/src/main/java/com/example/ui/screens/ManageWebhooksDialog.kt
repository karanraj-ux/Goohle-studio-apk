package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.shield.WebhookConfig
import kotlinx.coroutines.launch
import com.example.shield.WebhookWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.repository.WebhookRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWebhooksDialog(webhookRepository: WebhookRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val webhooks by webhookRepository.allWebhooks.collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf<WebhookConfig?>(null) }
    var isEditingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = { Text("Advanced Webhooks") },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                if (webhooks.isEmpty()) {
                    Text("No advanced webhooks configured.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(webhooks) { webhook ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(webhook.name, fontWeight = FontWeight.Bold)
                                        Text("${webhook.method} ${webhook.url}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            // Test Webhook
                                            val data = Data.Builder()
                                                .putString("url", webhook.url)
                                                .putString("title", "Test Sender")
                                                .putString("message", "This is a test message from KjAiShield")
                                                .putString("type", "TEST")
                                                .putString("method", webhook.method)
                                                .putString("headersJson", webhook.headersJson)
                                                .putString("customPayload", webhook.customPayload)
                                                .build()
                                            val request = OneTimeWorkRequestBuilder<WebhookWorker>().setInputData(data).build()
                                            WorkManager.getInstance(context).enqueue(request)
                                            android.widget.Toast.makeText(context, "Test triggered", android.widget.Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Test")
                                        }
                                        IconButton(onClick = {
                                            showEditDialog = webhook
                                            isEditingNew = false
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                webhookRepository.deleteWebhook(webhook)
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                showEditDialog = WebhookConfig(name = "New Webhook", url = "")
                isEditingNew = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Webhook")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showEditDialog != null) {
        EditWebhookDialog(
            webhook = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onSave = { updatedWebhook ->
                coroutineScope.launch {
                    webhookRepository.insertWebhook(updatedWebhook)
                }
                showEditDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWebhookDialog(webhook: WebhookConfig, onDismiss: () -> Unit, onSave: (WebhookConfig) -> Unit) {
    var name by remember { mutableStateOf(webhook.name) }
    var url by remember { mutableStateOf(webhook.url) }
    var method by remember { mutableStateOf(webhook.method) }
    var headersJson by remember { mutableStateOf(webhook.headersJson) }
    var customPayload by remember { mutableStateOf(webhook.customPayload) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text("Edit Webhook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name (e.g. Discord, HomeAssistant)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = method, onValueChange = {}, readOnly = true,
                        label = { Text("HTTP Method") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("POST", "GET", "PUT").forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { method = m; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = headersJson, onValueChange = { headersJson = it },
                    label = { Text("Custom Headers (JSON)") },
                    placeholder = { Text("{\"Authorization\": \"Bearer token\"}") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customPayload, onValueChange = { customPayload = it },
                    label = { Text("Custom Payload Builder") },
                    placeholder = { Text("{\"content\": \"{sender}: {message}\"}") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    supportingText = { Text("Use {sender}, {message}, {type}. Leave blank for default JSON.") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(webhook.copy(
                    name = name, url = url, method = method, headersJson = headersJson, customPayload = customPayload
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
