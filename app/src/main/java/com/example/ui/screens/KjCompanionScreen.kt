package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ShieldApplication
import com.example.data.ChatMessageEntity
import com.example.ui.viewmodels.KjCompanionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KjCompanionScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: KjCompanionViewModel = viewModel(
        factory = KjCompanionViewModel.Factory(
            (LocalContext.current.applicationContext as ShieldApplication).container.chatRepository,
            (LocalContext.current.applicationContext as ShieldApplication).container.callJobRepository,
            (LocalContext.current.applicationContext as ShieldApplication).container.smsRepository,
            (LocalContext.current.applicationContext as ShieldApplication).container.financialRepository
        )
    )
) {
    val context = LocalContext.current
    
    var input by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val messages = uiState.messages
    
    val settingsRepo = (context.applicationContext as ShieldApplication).container.settingsRepository
    val hasWelcomedKj by settingsRepo.hasWelcomedKj.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    
    val isTyping by viewModel.isTyping.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    // Initial welcome message
    LaunchedEffect(hasWelcomedKj) {
        if (!hasWelcomedKj) {
            viewModel.insertMessage(ChatMessageEntity(text = "Hello! I am KJ, your Retrospective AI. I have read-only access to your local SQLite database (including logs, calls, and finances) to answer your questions. \n\nHow can I help you today?", isUser = false, timestamp = System.currentTimeMillis()))
            settingsRepo.updateBoolean(com.example.data.repository.SettingsRepository.HAS_WELCOMED_KJ, true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "KJ AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "KJ Companion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            actions = {
                IconButton(onClick = { viewModel.clearAllMessages() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Privacy Shield", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Local & Private", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("KJ only reads local SQLite logs when asked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true
        ) {
            apiError?.let { errorMsg ->
                item {
                    ActionableErrorCard(
                        errorMessage = errorMsg,
                        onDismiss = { viewModel.dismissError() },
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
            if (isTyping) {
                item {
                    TypingIndicatorBubble()
                }
            }
            items(messages.reversed()) { message ->
                ChatBubble(message)
            }
            if (messages.size <= 1) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Start conversation", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Start a conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ask KJ to summarize your finances, analyze your calls, or write messages.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask KJ anything...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                FloatingActionButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            val userText = input
                            input = ""
                            viewModel.sendMessage(userText, context)
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "KJ AI", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = if (isUser) 24.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 24.dp
                    )
                )
                .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1f
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "KJ AI", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "KJ is thinking...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActionableErrorCard(errorMessage: String, onDismiss: () -> Unit, onNavigateToSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("API Connection Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Settings")
                }
            }
        }
    }
}