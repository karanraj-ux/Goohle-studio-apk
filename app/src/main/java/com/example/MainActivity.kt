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
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val container = (application as ShieldApplication).container
        MainViewModel.Factory(container.smsRepository, container.callJobRepository, container.financialRepository)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        
        val secureOtpText = intent.getStringExtra("SECURE_OTP_TEXT")
        val secureOtpTitle = intent.getStringExtra("SECURE_OTP_TITLE")
        
        var initialDeepLinkRule: String? = null
        if (intent?.action == Intent.ACTION_VIEW && intent?.scheme == "shield-app" && intent?.data?.host == "import") {
            initialDeepLinkRule = intent?.data?.getQueryParameter("rule")
        }

        setContent {
            MyApplicationTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val settingsRepo = (LocalContext.current.applicationContext as ShieldApplication).container.settingsRepository
                val coroutineScope = rememberCoroutineScope()
                val onboardingComplete by settingsRepo.onboardingComplete.collectAsState(initial = false)
                var hasCompletedOnboarding by remember { mutableStateOf(false) }
                
                LaunchedEffect(onboardingComplete) {
                    if (onboardingComplete) {
                        hasCompletedOnboarding = true
                    }
                }
                
                var showOnboarding = !onboardingComplete && !hasCompletedOnboarding

                if (showOnboarding) {
                    com.example.ui.screens.OnboardingScreen(onComplete = {
                        coroutineScope.launch {
                            settingsRepo.updateBoolean(com.example.data.repository.SettingsRepository.ONBOARDING_COMPLETE, true)
                        }
                        showOnboarding = false
                    })
                } else {
                    MainScreen(viewModel, windowSizeClass.widthSizeClass, secureOtpTitle, secureOtpText, initialDeepLinkRule)
                }
            }
        }
    }
}

sealed class Screen(val route: String, @androidx.annotation.StringRes val titleResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.title_dashboard, Icons.Default.Home)
    object KjAi : Screen("kj_ai", R.string.title_kj_ai, Icons.Default.Memory)
    object Declutter : Screen("declutter", R.string.title_declutter, Icons.Default.Delete)
    object Simulator : Screen("simulator", R.string.title_simulator, Icons.Default.PlayArrow)
    object Settings : Screen("settings", R.string.title_settings, Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, widthSizeClass: WindowWidthSizeClass, secureOtpTitle: String? = null, secureOtpText: String? = null, initialDeepLinkRule: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showSecureDialog by remember { mutableStateOf(secureOtpText != null) }

    val context = LocalContext.current
    val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
    
    val showAi by settingsRepo.showKjAi.collectAsState(initial = true)
    val showDeclutter by settingsRepo.showDeclutter.collectAsState(initial = true)

    val items = remember(showAi, showDeclutter) {
        listOf(Screen.Home) +
        listOf(Screen.KjAi to showAi, Screen.Declutter to showDeclutter)
            .filter { it.second }
            .map { it.first } +
        listOf(Screen.Simulator, Screen.Settings)
    }

    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    LaunchedEffect(Unit) {
        if (initialDeepLinkRule != null) {
            navController.navigate(Screen.KjAi.route)
        }
        com.example.shield.SystemNotificationEventBus.events.collect { event ->
            // Removed alert intent handling
        }
    }

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
                        icon = { Icon(screen.icon, contentDescription = androidx.compose.ui.res.stringResource(screen.titleResId)) },
                        label = { Text(androidx.compose.ui.res.stringResource(screen.titleResId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                                icon = { Icon(screen.icon, contentDescription = androidx.compose.ui.res.stringResource(screen.titleResId)) },
                                label = { Text(androidx.compose.ui.res.stringResource(screen.titleResId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                modifier = Modifier.padding(innerPadding),
                enterTransition = { 
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + 
                    androidx.compose.animation.slideInVertically(
                        initialOffsetY = { 50 }, 
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) 
                },
                exitTransition = { 
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) 
                },
                popEnterTransition = { 
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + 
                    androidx.compose.animation.slideInVertically(
                        initialOffsetY = { -50 }, 
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                },
                popExitTransition = { 
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) 
                }
            ) {
                composable(Screen.Home.route) { HomeScreen(viewModel) }
                composable(Screen.KjAi.route) { com.example.ui.screens.KjAiScreen(initialDeepLinkRule) }
                composable(Screen.Declutter.route) { com.example.ui.screens.DeclutterScreen(viewModel) }
                composable(Screen.Simulator.route) { com.example.ui.screens.SimulatorScreen(viewModel) }
                composable(Screen.Settings.route) { com.example.ui.screens.SettingsScreen() }
            }
            
            if (showSecureDialog && secureOtpText != null) {
                AlertDialog(
                    onDismissRequest = { showSecureDialog = false },
                    title = { Text(secureOtpTitle ?: "Secure OTP") },
                    text = { Text(secureOtpText) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
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
    val settingsRepo = (context.applicationContext as ShieldApplication).container.settingsRepository
    val settingsViewModel: com.example.ui.viewmodels.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.example.ui.viewmodels.SettingsViewModel.Factory(settingsRepo))
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    var permissionsGranted by remember { mutableStateOf(true) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forwardedToday = uiState.forwardedToday
    val totalForwarded = uiState.totalForwarded

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
        if (!permissionsGranted && settingsState.smsForwardingEnabled) {
            settingsViewModel.updateSmsForwardingEnabled(false)
            Toast.makeText(context, "Permissions required for auto-forwarding", Toast.LENGTH_LONG).show()
        }
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
                containerColor = if (settingsState.smsForwardingEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                        color = if (settingsState.smsForwardingEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (settingsState.smsForwardingEnabled) "Active & Monitoring" else "Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settingsState.smsForwardingEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val coroutineScope = rememberCoroutineScope()
                Switch(
                    checked = settingsState.smsForwardingEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !permissionsGranted) {
                            permissionLauncher.launch(permissions)
                        } else {
                            settingsViewModel.updateSmsForwardingEnabled(checked)
                            
                            val pm = context.packageManager
                            val compName = android.content.ComponentName(context, com.example.SmsReceiver::class.java)
                            val newState = if (checked) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            pm.setComponentEnabledSetting(compName, newState, android.content.pm.PackageManager.DONT_KILL_APP)
                            
                            if (!checked) {
                                viewModel.clearLogs()
                            }
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
                title = androidx.compose.ui.res.stringResource(R.string.home_today),
                value = forwardedToday.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = androidx.compose.ui.res.stringResource(R.string.home_total),
                value = totalForwarded.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        var targetNumbers = settingsState.targetNumbers
    
        // Auto-migrate legacy target_number if necessary for dashboard indicator
    LaunchedEffect(Unit) {
        if (targetNumbers.isBlank()) {
            val legacy = settingsRepo.targetNumbers.first()
            if (legacy.isNotBlank()) {
                settingsViewModel.updateTargetNumbers(legacy)
            }
        }
    }
        if (settingsState.smsForwardingEnabled && targetNumbers.isBlank()) {
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
        val showRecentLogs = settingsState.widgetRecentLogs
        val showQuickChat = settingsState.widgetQuickChat

        if (showRecentLogs) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val recentLogs = uiState.recentLogs
            Text(androidx.compose.ui.res.stringResource(R.string.home_recent_logs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (recentLogs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Inbox is squeaky clean", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Incoming messages will be analyzed and logged here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
            Text(androidx.compose.ui.res.stringResource(R.string.home_kj_quick_pin), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logs = uiState.recentLogs
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(
                        modifier = Modifier.size(96.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No logs yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("History of forwarded messages and actions will appear here once the Shield catches them.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
