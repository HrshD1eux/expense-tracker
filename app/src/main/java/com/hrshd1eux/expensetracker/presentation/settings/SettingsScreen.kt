package com.hrshd1eux.expensetracker.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hrshd1eux.expensetracker.BuildConfig
import com.hrshd1eux.expensetracker.data.preferences.AppTheme
import com.hrshd1eux.expensetracker.util.UpdateCheckResult
import com.hrshd1eux.expensetracker.util.UpdateChecker

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showWidgetInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf(
                        AppTheme.SYSTEM to "System default",
                        AppTheme.LIGHT to "Light theme",
                        AppTheme.DARK to "Dark theme"
                    ).forEach { (theme, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = state.theme == theme,
                                onClick = {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCurrencyDialog) {
        val currencies = listOf(
            Triple("INR", "₹", "Indian Rupee (INR)"),
            Triple("USD", "$", "US Dollar (USD)"),
            Triple("EUR", "€", "Euro (EUR)"),
            Triple("GBP", "£", "British Pound (GBP)"),
            Triple("JPY", "¥", "Japanese Yen (JPY)"),
            Triple("CAD", "$", "Canadian Dollar (CAD)"),
            Triple("AUD", "$", "Australian Dollar (AUD)")
        )

        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    Text(
                        text = "Note: Changing currency updates the display symbol across all records. Existing stored amounts will not be converted mathematically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    currencies.forEach { (code, symbol, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrency(code, symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = state.currencyCode == code,
                                onClick = {
                                    viewModel.setCurrency(code, symbol)
                                    showCurrencyDialog = false
                                }
                            )
                            Text(text = "$name ($symbol)", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showWidgetInfoDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetInfoDialog = false },
            title = { Text("Home Screen Widget") },
            text = {
                Text(
                    "You can add an Expense Tracker widget directly to your Android home screen!\n\n" +
                            "1. Long press on your phone's home screen.\n" +
                            "2. Tap 'Widgets'.\n" +
                            "3. Select 'Expense Tracker'.\n" +
                            "4. Place it on your screen to see today's spending at a glance and record expenses in 1 tap."
                )
            },
            confirmButton = {
                TextButton(onClick = { showWidgetInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Expense Tracker") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = com.hrshd1eux.expensetracker.R.mipmap.ic_launcher),
                        contentDescription = "Expense Tracker Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text(text = "Version ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "A 100% offline, privacy-first personal finance application.\n\n" +
                                "• Zero internet permissions for data\n" +
                                "• Zero analytics or third-party tracking\n" +
                                "• No accounts or registration required\n" +
                                "• Hardware-backed Keystore security\n" +
                                "• All data stays securely on your device."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDiagnosticsDialog) {
        val logs = remember { com.hrshd1eux.expensetracker.util.CrashLogger.getCrashLogs(context) }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = { Text("Diagnostics & Error Logs") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (logs.isBlank()) {
                        Text("No fatal crashes or errors recorded. The app is running smoothly.")
                    } else {
                        Text(
                            text = logs,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (logs.isNotBlank()) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs))
                            showDiagnosticsDialog = false
                        }
                    ) {
                        Text("Copy Logs")
                    }
                } else {
                    TextButton(onClick = { showDiagnosticsDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (logs.isNotBlank()) {
                    TextButton(
                        onClick = {
                            com.hrshd1eux.expensetracker.util.CrashLogger.clearCrashLogs(context)
                            showDiagnosticsDialog = false
                        }
                    ) {
                        Text("Clear Logs")
                    }
                }
            }
        )
    }

    when (val status = updateStatus) {
        is UpdateCheckResult.Checking -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Checking for Updates") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Text("Connecting to GitHub...")
                    }
                }
            )
        }
        is UpdateCheckResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearUpdateStatus() },
                title = { Text("App is Up to Date") },
                text = {
                    Text("You are using the latest version of Expense Tracker (v${BuildConfig.VERSION_NAME}).")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearUpdateStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateCheckResult.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearUpdateStatus() },
                title = { Text("Update Available: ${status.latestVersion}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = status.releaseTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (status.releaseNotes.isNotBlank()) {
                            Text(
                                text = status.releaseNotes.take(300) + if (status.releaseNotes.length > 300) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearUpdateStatus()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.downloadUrl ?: status.releaseHtmlUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Download")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearUpdateStatus() }) {
                        Text("Later")
                    }
                }
            )
        }
        is UpdateCheckResult.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearUpdateStatus() },
                title = { Text("Update Check Failed") },
                text = { Text(status.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearUpdateStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        UpdateCheckResult.Idle -> {}
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Appearance Section
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = when (state.theme) {
                        AppTheme.SYSTEM -> "System default"
                        AppTheme.LIGHT -> "Light theme"
                        AppTheme.DARK -> "Dark theme"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Dynamic Color",
                    subtitle = "Use colors derived from wallpaper (Android 12+)",
                    trailing = {
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    }
                )
            }

            // Security Section
            item {
                SettingsSectionHeader("Security")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = if (state.appLockEnabled) "Enabled · Biometric & PIN" else "Disabled",
                    onClick = onNavigateToSecurity
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Screen Privacy",
                    subtitle = "Block screenshots and recent-app previews",
                    trailing = {
                        Switch(
                            checked = state.screenSecurityEnabled,
                            onCheckedChange = { viewModel.setScreenSecurity(it) }
                        )
                    }
                )
            }

            // Categories Section
            item {
                SettingsSectionHeader("Categories")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    subtitle = "Add, rename, pick icons, archive & restore",
                    onClick = onNavigateToCategories
                )
            }

            // Currency Section
            item {
                SettingsSectionHeader("Currency")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.CurrencyRupee,
                    title = "Currency",
                    subtitle = "${state.currencyCode} (${state.currencySymbol})",
                    onClick = { showCurrencyDialog = true }
                )
            }

            // Backup Section
            item {
                SettingsSectionHeader("Backup")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Save,
                    title = "Export & Import",
                    subtitle = "Create or restore offline backups",
                    onClick = onNavigateToBackup
                )
            }

            // Widget Section
            item {
                SettingsSectionHeader("Widget")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Widgets,
                    title = "Widget Information",
                    subtitle = "Home screen glance widget guide",
                    onClick = { showWidgetInfoDialog = true }
                )
            }

            // About Section
            item {
                SettingsSectionHeader("About & Updates")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About Expense Tracker",
                    subtitle = "v${BuildConfig.VERSION_NAME} · 100% Offline & Private",
                    onClick = { showAboutDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = "Check GitHub releases for new version",
                    onClick = { viewModel.checkForUpdates() }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Developer GitHub",
                    subtitle = "HrshD1eux · Developer Profile & Source",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.GITHUB_DEV_PROFILE_URL))
                        context.startActivity(intent)
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Diagnostics & Error Logs",
                    subtitle = "Inspect and copy offline crash logs",
                    onClick = { showDiagnosticsDialog = true }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
