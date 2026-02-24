package smartracket.com.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import smartracket.com.R
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.model.Language
import smartracket.com.model.ThemeMode
import smartracket.com.repository.SyncState
import smartracket.com.ui.i18n.LocalAppStrings
import smartracket.com.ui.theme.SmartRacketColors
import smartracket.com.viewmodel.SettingsViewModel

/**
 * Settings Screen - App preferences and device management.
 *
 * Contains:
 * - Bluetooth device pairing
 * - User profile settings
 * - Health sync toggle
 * - App preferences
 * - Language toggle (English/Chinese)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isHealthConnectAvailable by viewModel.isHealthConnectAvailable.collectAsState()
    val hasHealthPermissions by viewModel.hasHealthPermissions.collectAsState()
    val isSamsungHealthConnected by viewModel.isSamsungHealthConnected.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val cloudSyncEnabled by viewModel.cloudSyncEnabled.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncStats by viewModel.syncStats.collectAsState()
    val deviceToRemove by viewModel.deviceToRemove.collectAsState()
    val strings = LocalAppStrings.current

    // Bluetooth permissions
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(bluetoothPermissions)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(strings.settingsTitle) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth Section
            SettingsSection(title = strings.bluetooth) {
                // Connection status
                val currentState = connectionState
                SettingsItem(
                    icon = when (currentState) {
                        is BluetoothConnectionState.Connected -> Icons.Default.BluetoothConnected
                        is BluetoothConnectionState.Connecting,
                        BluetoothConnectionState.Scanning -> Icons.Default.BluetoothSearching
                        else -> Icons.Default.BluetoothDisabled
                    },
                    title = strings.paddleConnection,
                    subtitle = when (currentState) {
                        is BluetoothConnectionState.Connected ->
                            "${strings.connectedTo} ${currentState.device.deviceName}"
                        is BluetoothConnectionState.Connecting -> "${strings.connectingTo} ${currentState.deviceName}..."
                        BluetoothConnectionState.Scanning -> strings.scanning
                        is BluetoothConnectionState.Error -> currentState.message
                        else -> strings.notConnectedLabel
                    },
                    iconTint = when (currentState) {
                        is BluetoothConnectionState.Connected -> SmartRacketColors.StatusConnected
                        is BluetoothConnectionState.Connecting,
                        BluetoothConnectionState.Scanning -> SmartRacketColors.StatusConnecting
                        is BluetoothConnectionState.Error -> SmartRacketColors.StatusError
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                ) {
                    if (currentState is BluetoothConnectionState.Connected) {
                        TextButton(onClick = { viewModel.disconnectDevice() }) {
                            Text(strings.disconnect)
                        }
                    } else if (currentState !is BluetoothConnectionState.Connecting &&
                               currentState !is BluetoothConnectionState.Scanning) {
                        if (permissionState.allPermissionsGranted) {
                            TextButton(onClick = { viewModel.startScan() }) {
                                Text(strings.scan)
                            }
                        } else {
                            TextButton(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                                Text(strings.grantPermission)
                            }
                        }
                    }
                }
            }

            // Paired Devices Section
            SettingsSection(title = strings.devices) {
                if (pairedDevices.isEmpty()) {
                    SettingsItem(
                        icon = Icons.Default.DevicesOther,
                        title = strings.noPairedDevices,
                        subtitle = strings.managePairedDevices
                    )
                } else {
                    pairedDevices.forEach { device ->
                        SettingsItem(
                            icon = painterResource(R.drawable.ic_table_tennis),
                            title = device.deviceName + if (device.isPrimary) " ★" else "",
                            subtitle = "${strings.lastConnected}: ${device.lastConnectedFormatted}"
                        ) {
                            Row {
                                if (!device.isPrimary) {
                                    TextButton(onClick = { viewModel.setPrimaryDevice(device.address) }) {
                                        Text(strings.setAsPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Text(
                                        text = strings.primaryDevice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.showRemoveDeviceDialog(device) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = strings.removeDevice,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Health Connect Section
            SettingsSection(title = strings.healthFitness) {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = strings.healthConnect,
                    subtitle = when {
                        !isHealthConnectAvailable -> strings.notAvailable
                        hasHealthPermissions -> strings.connectedSyncing
                        else -> strings.tapToConnect
                    },
                    iconTint = if (hasHealthPermissions) SmartRacketColors.HeartRatePink
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) {
                    if (isHealthConnectAvailable && !hasHealthPermissions) {
                        TextButton(onClick = { viewModel.requestHealthPermissions() }) {
                            Text(strings.connect)
                        }
                    }
                }

                // Samsung Health connection
                SettingsItem(
                    icon = Icons.Default.Watch,
                    title = strings.samsungHealth,
                    subtitle = if (isSamsungHealthConnected)
                        strings.samsungHealthConnected
                    else
                        strings.samsungHealthDescription,
                    iconTint = if (isSamsungHealthConnected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) {
                    TextButton(
                        onClick = {
                            if (isSamsungHealthConnected) viewModel.disconnectSamsungHealth()
                            else viewModel.connectSamsungHealth()
                        }
                    ) {
                        Text(
                            if (isSamsungHealthConnected) strings.disconnect
                            else strings.connect
                        )
                    }
                }
            }

            // Cloud Sync Section
            SettingsSection(title = strings.cloudSync) {
                if (!syncStats.isFirebaseConfigured) {
                    // Firebase not configured — show setup notice
                    SettingsItem(
                        icon = Icons.Default.CloudOff,
                        title = strings.firebaseNotConfigured,
                        subtitle = strings.firebaseNotConfiguredSubtitle,
                        iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                } else {
                    // Cloud sync toggle
                    SettingsSwitchItem(
                        icon = Icons.Default.Cloud,
                        title = strings.cloudSync,
                        subtitle = if (cloudSyncEnabled) strings.cloudSyncEnabled else strings.cloudSyncDisabled,
                        checked = cloudSyncEnabled,
                        onCheckedChange = { viewModel.setCloudSyncEnabled(it) }
                    )

                    // Sync status
                    val syncStatusText = when (val state = syncState) {
                        is SyncState.Idle -> "${strings.syncedSessions}: ${syncStats.syncedSessions} · ${strings.pendingSync}: ${syncStats.pendingSessions}"
                        is SyncState.Syncing -> strings.syncing
                        is SyncState.Success -> "${strings.syncSuccess} (${state.sessionsSynced})"
                        is SyncState.Error -> "${strings.syncError}: ${state.message}"
                        is SyncState.FirebaseUnavailable -> strings.firebaseNotConfigured
                    }

                    SettingsItem(
                        icon = Icons.Default.Sync,
                        title = strings.syncStatus,
                        subtitle = syncStatusText,
                        iconTint = when (syncState) {
                            is SyncState.Syncing -> SmartRacketColors.StatusConnecting
                            is SyncState.Success -> SmartRacketColors.StatusConnected
                            is SyncState.Error -> SmartRacketColors.StatusError
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    ) {
                        if (cloudSyncEnabled && syncState !is SyncState.Syncing) {
                            TextButton(onClick = { viewModel.syncNow() }) {
                                Text(strings.syncNow)
                            }
                        }
                    }
                }
            }

            // Training Settings
            SettingsSection(title = strings.trainingSection) {
                // Keep screen on
                SettingsSwitchItem(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = strings.keepScreenOn,
                    subtitle = strings.keepScreenOnSubtitle,
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )

                // Vibration
                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = strings.vibrationFeedback,
                    subtitle = strings.vibrationSubtitle,
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }

            // Language Settings
            SettingsSection(title = strings.languageLabel) {
                LanguageSelectorItem(
                    currentLanguage = language,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }

            // Appearance Settings (Theme Mode)
            SettingsSection(title = strings.appearance) {
                ThemeModeSelectorItem(
                    currentMode = themeMode,
                    strings = strings,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )
            }

            // App Info
            SettingsSection(title = strings.about) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = strings.appName,
                    subtitle = "${strings.version} 1.0.0"
                )

                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = strings.privacyPolicy,
                    subtitle = strings.viewPrivacy,
                    onClick = { viewModel.openPrivacyPolicy() }
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = strings.termsOfService,
                    subtitle = strings.viewTerms,
                    onClick = { viewModel.openTerms() }
                )

                SettingsItem(
                    icon = Icons.Default.Feedback,
                    title = strings.sendFeedback,
                    subtitle = strings.helpImprove,
                    onClick = { viewModel.sendFeedback() }
                )
            }

            // Data Management
            SettingsSection(title = strings.dataSection) {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = strings.clearAllData,
                    subtitle = strings.clearAllDataSubtitle,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.showClearDataDialog() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear data confirmation dialog
    val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDataDialog() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(strings.clearAllDataTitle) },
            text = {
                Text(strings.clearAllDataMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllData() }
                ) {
                    Text(strings.clearAll, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDataDialog() }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Remove device confirmation dialog
    if (deviceToRemove != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRemoveDeviceDialog() },
            icon = { Icon(Icons.Default.BluetoothDisabled, contentDescription = null) },
            title = { Text(strings.removeDeviceTitle) },
            text = {
                Text(strings.removeDeviceMessage)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRemoveDevice() }) {
                    Text(strings.remove, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRemoveDeviceDialog() }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        trailing?.invoke()
    }
}

@Composable
private fun SettingsItem(
    icon: Painter,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        trailing?.invoke()
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

@Composable
private fun LanguageSelectorItem(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        val strings = LocalAppStrings.current
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.languageLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = currentLanguage.nativeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = lang.nativeName,
                                fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                            )
                            if (lang != Language.ENGLISH) {
                                Text(
                                    text = "(${lang.displayName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    },
                    leadingIcon = {
                        if (lang == currentLanguage) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelectorItem(
    currentMode: ThemeMode,
    strings: smartracket.com.ui.i18n.AppStrings,
    onModeSelected: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (currentMode) {
                ThemeMode.LIGHT -> Icons.Default.LightMode
                ThemeMode.DARK -> Icons.Default.DarkMode
                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.themeMode,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (currentMode) {
                    ThemeMode.SYSTEM -> strings.themeSystem
                    ThemeMode.LIGHT -> strings.themeLight
                    ThemeMode.DARK -> strings.themeDark
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> strings.themeSystem
                                ThemeMode.LIGHT -> strings.themeLight
                                ThemeMode.DARK -> strings.themeDark
                            },
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = null,
                            tint = if (mode == currentMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }
    }
}
