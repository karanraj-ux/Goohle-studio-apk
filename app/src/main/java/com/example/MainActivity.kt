package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.data.SmsLogEntity
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Dashboard", Icons.Default.Home)
    object Simulator : Screen("simulator", "Test", Icons.Default.PlayArrow)
    object Logs : Screen("logs", "History", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(Screen.Home, Screen.Simulator, Screen.Logs, Screen.Settings)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Simulator.route) { com.example.ui.screens.SimulatorScreen(viewModel) }
            composable(Screen.Logs.route) { LogsScreen(viewModel) }
            composable(Screen.Settings.route) { com.example.ui.screens.SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE) }
    
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("is_enabled", false)) }
    var permissionsGranted by remember { mutableStateOf(true) }
    val forwardedToday by viewModel.forwardedToday.collectAsStateWithLifecycle()
    val totalForwarded by viewModel.totalForwarded.collectAsStateWithLifecycle()

    val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted && isEnabled) {
            isEnabled = false
            prefs.edit().putBoolean("is_enabled", false).apply()
            Toast.makeText(context, "Permissions required for auto-forwarding", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Master Switch Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Forwarding",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isEnabled) "Active & Monitoring" else "Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !permissionsGranted) {
                            permissionLauncher.launch(permissions)
                        } else {
                            isEnabled = checked
                            prefs.edit().putBoolean("is_enabled", checked).apply()
                        }
                    }
                )
            }
        }

        if (!permissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Permissions missing! Forwarding cannot work.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Today",
                value = forwardedToday.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total",
                value = totalForwarded.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        var targetNumbers by remember { mutableStateOf(prefs.getString("target_numbers", "") ?: "") }
    
    // Auto-migrate legacy target_number if necessary for dashboard indicator
    LaunchedEffect(Unit) {
        if (targetNumbers.isBlank()) {
            val legacy = prefs.getString("target_number", "") ?: ""
            if (legacy.isNotBlank()) {
                targetNumbers = legacy
                prefs.edit().putString("target_numbers", legacy).apply()
            }
        }
    }
        if (isEnabled && targetNumbers.isBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "No Target Number set! Go to Settings to configure where to forward messages.",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("History") },
            actions = {
                if (logs.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            }
        )

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.List, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No messages forwarded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItem(log, sdf)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: SmsLogEntity, sdf: SimpleDateFormat) {
    val isSuccess = log.status == "SUCCESS"
    val isIgnored = log.status == "IGNORED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.sender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when {
                                isSuccess -> MaterialTheme.colorScheme.primary
                                isIgnored -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = log.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isSuccess -> MaterialTheme.colorScheme.primary
                            isIgnored -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sdf.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSuccess) {
                    Text(
                        text = "→ ${log.targetNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
