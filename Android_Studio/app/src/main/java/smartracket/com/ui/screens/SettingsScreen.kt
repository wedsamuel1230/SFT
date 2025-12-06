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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import smartracket.com.model.BluetoothConnectionState
import smartracket.com.viewmodel.SettingsViewModel

/**
 * Settings Screen - App preferences and device management.
 *
 * Contains:
 * - Bluetooth device pairing
 * - User profile settings
 * - Health sync toggle
 * - Auto-save highlight threshold
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
    val autoSaveThreshold by viewModel.autoSaveThreshold.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val isChineseLanguage by viewModel.isChineseLanguage.collectAsState()

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
            title = { Text(if (isChineseLanguage) "设置" else "Settings") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth Section
            SettingsSection(title = "Bluetooth") {
                // Connection status
                val currentState = connectionState
                SettingsItem(
                    icon = when (currentState) {
                        is BluetoothConnectionState.Connected -> Icons.Default.BluetoothConnected
                        is BluetoothConnectionState.Connecting,
                        BluetoothConnectionState.Scanning -> Icons.Default.BluetoothSearching
                        else -> Icons.Default.BluetoothDisabled
                    },
                    title = "Paddle Connection",
                    subtitle = when (currentState) {
                        is BluetoothConnectionState.Connected ->
                            "Connected to ${currentState.device.deviceName}"
                        is BluetoothConnectionState.Connecting -> "Connecting to ${currentState.deviceName}..."
                        BluetoothConnectionState.Scanning -> "Scanning..."
                        is BluetoothConnectionState.Error -> currentState.message
                        else -> "Not connected"
                    },
                    iconTint = when (currentState) {
                        is BluetoothConnectionState.Connected -> Color(0xFF4CAF50)
                        is BluetoothConnectionState.Connecting,
                        BluetoothConnectionState.Scanning -> Color(0xFF2196F3)
                        is BluetoothConnectionState.Error -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                ) {
                    if (currentState is BluetoothConnectionState.Connected) {
                        TextButton(onClick = { viewModel.disconnectDevice() }) {
                            Text("Disconnect")
                        }
                    } else if (currentState !is BluetoothConnectionState.Connecting &&
                               currentState !is BluetoothConnectionState.Scanning) {
                        if (permissionState.allPermissionsGranted) {
                            TextButton(onClick = { viewModel.startScan() }) {
                                Text("Scan")
                            }
                        } else {
                            TextButton(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Paired devices
                if (pairedDevices.isNotEmpty()) {
                    pairedDevices.forEach { device ->
                        SettingsItem(
                            icon = Icons.Default.SportsTennis,
                            title = device.deviceName,
                            subtitle = "Last connected: ${device.lastConnectedFormatted}"
                        ) {
                            TextButton(onClick = { viewModel.connectToDevice(device.address) }) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }

            // Health Connect Section
            SettingsSection(title = "Health & Fitness") {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = "Health Connect",
                    subtitle = when {
                        !isHealthConnectAvailable -> "Not available on this device"
                        hasHealthPermissions -> "Connected - syncing heart rate data"
                        else -> "Tap to connect"
                    },
                    iconTint = if (hasHealthPermissions) Color(0xFFE91E63)
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) {
                    if (isHealthConnectAvailable && !hasHealthPermissions) {
                        TextButton(onClick = { viewModel.requestHealthPermissions() }) {
                            Text("Connect")
                        }
                    }
                }
            }

            // Training Settings
            SettingsSection(title = "Training") {
                // Auto-save threshold
                SettingsSliderItem(
                    icon = Icons.Default.Stars,
                    title = "Auto-save Highlight Threshold",
                    subtitle = "Score $autoSaveThreshold and above",
                    value = autoSaveThreshold.toFloat(),
                    valueRange = 5f..10f,
                    steps = 4,
                    onValueChange = { viewModel.setAutoSaveThreshold(it.toInt()) }
                )

                // Keep screen on
                SettingsSwitchItem(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off during training",
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )

                // Vibration
                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = if (isChineseLanguage) "振动反馈" else "Vibration Feedback",
                    subtitle = if (isChineseLanguage) "击球检测时振动" else "Vibrate on stroke detection",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }

            // Language Settings
            SettingsSection(title = if (isChineseLanguage) "语言" else "Language") {
                SettingsSwitchItem(
                    icon = Icons.Default.Language,
                    title = if (isChineseLanguage) "中文模式" else "Chinese Mode",
                    subtitle = if (isChineseLanguage) "切换到英文 / Switch to English" else "切换到中文 / Switch to Chinese",
                    checked = isChineseLanguage,
                    onCheckedChange = { viewModel.setChineseLanguage(it) }
                )
            }

            // App Info
            SettingsSection(title = if (isChineseLanguage) "关于" else "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "SmartRacket Coach",
                    subtitle = if (isChineseLanguage) "版本 1.0.0" else "Version 1.0.0"
                )

                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = if (isChineseLanguage) "隐私政策" else "Privacy Policy",
                    subtitle = if (isChineseLanguage) "查看隐私政策" else "View our privacy policy",
                    onClick = { viewModel.openPrivacyPolicy() }
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = if (isChineseLanguage) "服务条款" else "Terms of Service",
                    subtitle = if (isChineseLanguage) "查看服务条款" else "View terms and conditions",
                    onClick = { viewModel.openTerms() }
                )

                SettingsItem(
                    icon = Icons.Default.Feedback,
                    title = if (isChineseLanguage) "发送反馈" else "Send Feedback",
                    subtitle = if (isChineseLanguage) "帮助我们改进应用" else "Help us improve the app",
                    onClick = { viewModel.sendFeedback() }
                )
            }

            // Data Management
            SettingsSection(title = if (isChineseLanguage) "数据" else "Data") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = if (isChineseLanguage) "清除所有数据" else "Clear All Data",
                    subtitle = if (isChineseLanguage) "删除所有训练记录和精彩片段" else "Delete all training sessions and highlights",
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
            title = { Text("Clear All Data?") },
            text = {
                Text("This will permanently delete all training sessions, strokes, and highlights. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllData() }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDataDialog() }) {
                    Text("Cancel")
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


