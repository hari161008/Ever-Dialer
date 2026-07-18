package com.supernova.networkswitch.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.SettingsViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SettingsActivity : ComponentActivity() {
    
    private val viewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                NetworkSwitchGraph.init(applicationContext)
                return SettingsViewModel(
                    NetworkSwitchGraph.preferencesRepository,
                    NetworkSwitchGraph.networkControlRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        NetworkSwitchGraph.init(applicationContext)
        com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult = {
            runOnUiThread { viewModel.refreshCompatibility() }
        }
        
        setContent {
            NetworkSwitchTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCompatibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult != null) {
            com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val controlMethod by viewModel.controlMethod.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Method Selection
            ControlMethodCard(
                selectedMethod = controlMethod,
                onMethodSelected = { viewModel.updateControlMethod(it) },
                rootCompatibility = viewModel.rootCompatibility,
                shizukuCompatibility = viewModel.shizukuCompatibility,
                onRetryClick = { viewModel.retryCompatibilityCheck() }
            )
            
            // About Section
            AboutCard()
        }
    }
}

@Composable
private fun ControlMethodCard(
    selectedMethod: ControlMethod,
    onMethodSelected: (ControlMethod) -> Unit,
    rootCompatibility: CompatibilityState,
    shizukuCompatibility: CompatibilityState,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = com.supernova.networkswitch.presentation.ui.components.NetworkSwitchCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                FilledTonalIconButton(onClick = onRetryClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh compatibility"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose how the app should control network settings. Root method requires a rooted device, while Shizuku method works with non-rooted devices that have Shizuku installed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Root Method Option
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                color = if (selectedMethod == ControlMethod.ROOT)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMethod == ControlMethod.ROOT,
                        onClick = { onMethodSelected(ControlMethod.ROOT) }
                    )
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMethod == ControlMethod.ROOT,
                    onClick = { onMethodSelected(ControlMethod.ROOT) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Root Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Requires rooted device with root access granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Compatibility status indicator
                when (rootCompatibility) {
                    is CompatibilityState.Pending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is CompatibilityState.Compatible -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Compatible",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is CompatibilityState.PermissionDenied -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Permission denied",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is CompatibilityState.Incompatible -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Shizuku Method Option
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                color = if (selectedMethod == ControlMethod.SHIZUKU)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMethod == ControlMethod.SHIZUKU,
                        onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
                    )
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMethod == ControlMethod.SHIZUKU,
                    onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shizuku Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Works with non-rooted devices using Shizuku service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Compatibility status indicator
                when (shizukuCompatibility) {
                    is CompatibilityState.Pending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is CompatibilityState.Compatible -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Compatible",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is CompatibilityState.PermissionDenied -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Permission denied",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is CompatibilityState.Incompatible -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Not available",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = com.supernova.networkswitch.presentation.ui.components.NetworkSwitchCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Source Code",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinkItem(
                title = "NetworkSwitch",
                subtitle = "https://github.com/aunchagaonkar/NetworkSwitch",
                link = "https://github.com/aunchagaonkar/NetworkSwitch"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Open Source Licenses",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinkItem(
                title = "Shizuku",
                subtitle = "Apache License 2.0\nhttps://github.com/RikkaApps/Shizuku",
                link = "https://github.com/RikkaApps/Shizuku"
            )
            
            LinkItem(
                title = "libsu",
                subtitle = "Apache License 2.0\nhttps://github.com/topjohnwu/libsu",
                link = "https://github.com/topjohnwu/libsu"
            )
            
            LinkItem(
                title = "Android Jetpack",
                subtitle = "Apache License 2.0\nhttps://android.googlesource.com/platform/frameworks/support",
                link = "https://android.googlesource.com/platform/frameworks/support"
            )
            
            LinkItem(
                title = "Kotlin",
                subtitle = "Apache License 2.0\nhttps://github.com/JetBrains/kotlin",
                link = "https://github.com/JetBrains/kotlin"
            )
        }
    }
}

@Composable
private fun LinkItem(
    title: String,
    subtitle: String,
    link: String
) {
    val context = LocalContext.current
    
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
