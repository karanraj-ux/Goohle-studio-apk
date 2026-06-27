package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclutterScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptions = uiState.subscriptions
    val expenses = uiState.expenses
    val totalSpent = uiState.totalSpent

    var selectedTab by remember { mutableStateOf(0) }
    
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.scanFinancialData(context)
            } else {
                Toast.makeText(context, "SMS reading permission required to scan", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Finances",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan SMS")
            }
        }

        // Summary Cards
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Spent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("₹${String.format("%.2f", totalSpent ?: 0.0)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Active Subs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${subscriptions.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        
        TabRow(
            selectedTabIndex = selectedTab, 
            modifier = Modifier.padding(bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Expenses") }, icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Expenses") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Subscriptions") }, icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Subscriptions") })
        }

        if (selectedTab == 0) {
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Receipt, contentDescription = "Receipt", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No recent expenses",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Financial SMS will appear here",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(expenses) { exp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.merchant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Amount: ${exp.amountStr}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Detected: ${formatDate(exp.dateDetected)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (subscriptions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No active subscriptions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap Scan Inbox to detect reoccurring charges",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(subscriptions) { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = "Monetization", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Amount: ${sub.amount}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Detected: ${formatDate(sub.dateDetected)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSubscription(sub.id) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Delete, "Dismiss", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timeMs: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timeMs))
}
