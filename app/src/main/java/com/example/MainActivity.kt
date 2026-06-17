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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.data.SmsLogEntity
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val serviceIntent = Intent(this, com.example.shield.ShieldCoreService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        val secureOtpText = intent.getStringExtra("SECURE_OTP_TEXT")
        val secureOtpTitle = intent.getStringExtra("SECURE_OTP_TITLE")

        setContent {
            MyApplicationTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val prefs = remember { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_complete", false)) }

                if (showOnboarding) {
                    com.example.ui.screens.OnboardingScreen(onComplete = {
                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                        showOnboarding = false
                    })
                } else {
                    MainScreen(viewModel, windowSizeClass.widthSizeClass, secureOtpTitle, secureOtpText)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Dashboard", Icons.Default.Home)
    object KjCompanion : Screen("kj_companion", "KJ Chat", Icons.AutoMirrored.Filled.Chat)
    object KjAi : Screen("kj_ai", "Engine", Icons.Default.Memory)
    object Calls : Screen("calls", "Auto Call", Icons.Default.Call)
    object Shield : Screen("shield", "Shield", Icons.Default.Security)
    object Declutter : Screen("declutter", "Declutter", Icons.Default.Delete)
    object Simulator : Screen("simulator", "Test", Icons.Default.PlayArrow)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, widthSizeClass: WindowWidthSizeClass, secureOtpTitle: String? = null, secureOtpText: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showSecureDialog by remember { mutableStateOf(secureOtpText != null) }

    val context = LocalContext.current
    val appUiPrefs = remember { context.getSharedPreferences("app_ui_prefs", Context.MODE_PRIVATE) }
    
    var items by remember { mutableStateOf(emptyList<Screen>()) }
    
    // Listen for changes
    DisposableEffect(appUiPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            val newItems = listOf(Screen.Home) +
                    listOf(Screen.KjCompanion, Screen.KjAi, Screen.Calls, Screen.Shield, Screen.Declutter).filter {
                        appUiPrefs.getBoolean("show_${it.route}", true)
                    } +
                    listOf(Screen.Simulator, Screen.Settings)
            items = newItems
        }
        appUiPrefs.registerOnSharedPreferenceChangeListener(listener)
        // Initial setup
        listener.onSharedPreferenceChanged(appUiPrefs, null)
        onDispose { appUiPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            NavigationRail(
                modifier = Modifier.width(96.dp).fillMaxHeight(),
                header = {
                    Box(
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 12.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) {
                Spacer(Modifier.height(16.dp))
                items.forEach { screen ->
                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeScreen(viewModel) }
                composable(Screen.KjCompanion.route) { com.example.ui.screens.KjCompanionScreen() }
                composable(Screen.KjAi.route) { com.example.ui.screens.KjAiScreen() }
                composable(Screen.Calls.route) { com.example.ui.screens.CallsScreen(viewModel) }
                composable(Screen.Shield.route) { com.example.ui.screens.ShieldScreen() }
                composable(Screen.Declutter.route) { com.example.ui.screens.DeclutterScreen(viewModel) }
                composable(Screen.Simulator.route) { com.example.ui.screens.SimulatorScreen(viewModel) }
                composable(Screen.Settings.route) { com.example.ui.screens.SettingsScreen() }
            }
            
            if (showSecureDialog && secureOtpText != null) {
                AlertDialog(
                    onDismissRequest = { showSecureDialog = false },
                    title = { Text(secureOtpTitle ?: "Secure OTP") },
                    text = { Text(secureOtpText) },
                    icon = { Icon(Icons.Default.Security, null) },
                    confirmButton = {
                        Button(onClick = { showSecureDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
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

        // Background Reliability Check Card (OEM Tracker)
        val shieldActive = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val ignoresBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (shieldActive && ignoresBattery) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (shieldActive && ignoresBattery) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Shield Status",
                        tint = if (shieldActive && ignoresBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Background Protection Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (shieldActive && ignoresBattery) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background((if (shieldActive && ignoresBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.2f)))

                // Active Checklists
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Shield Service",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (shieldActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (shieldActive) "Interceptor Online" else "Disabled: Threat & OTP blocking not functional",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!shieldActive) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Enable Access", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OEM Power Protection",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (ignoresBattery) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (ignoresBattery) "Bypassed: Safe from background killer" else "Restrained: Background saver may freeze protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!ignoresBattery) {
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Bypass Saver", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Bypassed", tint = MaterialTheme.colorScheme.primary)
                    }
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

        // --- Customizable Widgets ---
        val showRecentLogs = prefs.getBoolean("widget_recent_logs", true)
        val showQuickChat = prefs.getBoolean("widget_quick_chat", true)

        if (showRecentLogs) {
            val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
            Text("Recent Inbox Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (recentLogs.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Inbox is empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        recentLogs.take(3).forEach { log ->
                            Text(log.sender, fontWeight = FontWeight.Bold)
                            Text(log.message, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }

        if (showQuickChat) {
            Text("KJ Quick Pin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KJ is waiting for your next command...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
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
