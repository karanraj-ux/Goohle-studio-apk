package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KjCompanionScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.chatMessageDao()
    
    var input by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    val messages by dao.getAllMessages().collectAsState(initial = emptyList())
    val appUiPrefs = remember { context.getSharedPreferences("app_ui_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Initial welcome message
    LaunchedEffect(Unit) {
        if (!appUiPrefs.getBoolean("has_welcomed_kj", false)) {
            dao.insertMessage(ChatMessageEntity(text = "Hello! I am KJ, your personal assistant. How can I help you configure your automated rules or summarize your logs today?", isUser = false, timestamp = System.currentTimeMillis()))
            appUiPrefs.edit().putBoolean("has_welcomed_kj", true).apply()
        }
    }

    val calls by database.callJobDao().getAllJobsFlow().collectAsState(initial = emptyList())
    val sms by database.smsLogDao().getRecentLogs().collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
    val subs by database.subscriptionDao().getFinancialSubscriptions().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = "KJ", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("KJ Companion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Online • Gemini API Engine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            actions = {
                IconButton(onClick = { coroutineScope.launch { dao.clearAllMessages() } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask KJ a question...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (input.isNotBlank()) {
                        val userText = input
                        input = ""
                        
                        coroutineScope.launch {
                            dao.insertMessage(ChatMessageEntity(text = userText, isUser = true, timestamp = System.currentTimeMillis()))
                            
                            val logsContext = """
                                SMS Logs: ${sms.take(5).joinToString { "[${it.sender}: ${it.message.take(50)}]" }}
                                Auto Call Rules: ${calls.joinToString { "[${it.description} to ${it.phoneNumber} next at ${it.nextCallTime}]" }}
                                Subscriptions: ${subs.joinToString { "[${it.name}: ${it.amount}]" }}
                                Expenses: ${expenses.joinToString { "[${it.merchant}: ${it.amountStr}]" }}
                            """.trimIndent()
                            
                            val responseText = com.example.network.generateContentWithHistory(messages, userText, logsContext)
                            dao.insertMessage(ChatMessageEntity(text = responseText, isUser = false, timestamp = System.currentTimeMillis()))
                        }
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "KJ AI", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
