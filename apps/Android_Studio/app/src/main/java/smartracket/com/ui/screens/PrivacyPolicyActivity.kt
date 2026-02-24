package smartracket.com.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import smartracket.com.ui.theme.SmartRacketTheme

/**
 * Privacy Policy Activity for Health Connect integration.
 *
 * Required by Health Connect to show users what data the app accesses
 * and how it's used.
 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartRacketTheme {
                PrivacyPolicyScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SmartRacket Coach Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Last updated: December 2024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PolicySection(
                title = "Information We Collect",
                content = """
                    SmartRacket Coach collects the following data:
                    
                    • Motion Data: Accelerometer and gyroscope data from your connected SmartRacket paddle to analyze your table tennis strokes.
                    
                    • Health Data: If you grant permission, we read heart rate data from Health Connect/Samsung Health to provide insights about your training intensity.
                    
                    • Training Sessions: We store your training session data including stroke counts, scores, and timestamps locally on your device.
                """.trimIndent()
            )

            PolicySection(
                title = "How We Use Your Data",
                content = """
                    • Stroke Analysis: Motion data is processed locally using machine learning to classify your strokes and provide real-time feedback.
                    
                    • Performance Tracking: Training data is used to show your progress over time.
                    
                    • Health Insights: Heart rate data is used to correlate training intensity with performance.
                    
                    All data processing happens locally on your device. We do not transmit your personal data to external servers.
                """.trimIndent()
            )

            PolicySection(
                title = "Health Connect Data",
                content = """
                    When you connect Health Connect, SmartRacket Coach:
                    
                    • Reads heart rate data during and after training sessions
                    • Reads exercise session data for historical analysis
                    • Writes exercise sessions to track your table tennis training
                    
                    You can revoke these permissions at any time through the Health Connect app settings.
                """.trimIndent()
            )

            PolicySection(
                title = "Data Storage",
                content = """
                    All your training data is stored locally on your device using encrypted storage. Your data is not uploaded to any cloud service unless you explicitly choose to share it.
                    
                    You can delete all your data at any time from the Settings screen within the app.
                """.trimIndent()
            )

            PolicySection(
                title = "Data Sharing",
                content = """
                    We do not sell or share your personal data with third parties. 
                    
                    You may choose to share individual highlights through the app's sharing feature, which uses your device's standard sharing functionality.
                """.trimIndent()
            )

            PolicySection(
                title = "Contact Us",
                content = """
                    If you have questions about this privacy policy or our data practices, please contact us at:
                    
                    privacy@smartracket.com
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

