package smartracket.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import smartracket.com.model.ThemeMode
import smartracket.com.ui.i18n.LocalAppStrings
import smartracket.com.ui.i18n.Strings
import smartracket.com.ui.screens.*
import smartracket.com.ui.theme.SmartRacketTheme
import smartracket.com.viewmodel.SettingsViewModel

/**
 * Main entry point for SmartRacket Coach app.
 *
 * Sets up:
 * - Jetpack Compose UI with Material 3
 * - Navigation with bottom navigation bar
 * - Hilt dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRacketApp()
        }
    }
}

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Training : Screen("training", "Training", Icons.Default.FitnessCenter)
    data object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    data object Highlights : Screen("highlights", "Highlights", Icons.Default.Stars)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

/**
 * Main app composable with navigation setup.
 *
 * Provides [LocalAppStrings] at the composition root so all child composables
 * can access localized strings via `LocalAppStrings.current`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRacketApp(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val language by settingsViewModel.language.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val strings = Strings.forLanguage(language)

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    SmartRacketTheme(darkTheme = darkTheme) {
    CompositionLocalProvider(LocalAppStrings provides strings) {
        val navController = rememberNavController()

        val bottomNavItems = listOf(
            Screen.Home,
            Screen.Training,
            Screen.Analytics,
            Screen.Highlights,
            Screen.Settings
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val currentStrings = LocalAppStrings.current

                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = when (screen) {
                                        Screen.Home -> currentStrings.home
                                        Screen.Training -> currentStrings.training
                                        Screen.Analytics -> currentStrings.analytics
                                        Screen.Highlights -> currentStrings.highlights
                                        Screen.Settings -> currentStrings.settings
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = when (screen) {
                                        Screen.Home -> currentStrings.home
                                        Screen.Training -> currentStrings.training
                                        Screen.Analytics -> currentStrings.analytics
                                        Screen.Highlights -> currentStrings.highlights
                                        Screen.Settings -> currentStrings.settings
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (screen == Screen.Home) {
                                    // Home always clears the entire back stack
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartTraining = {
                            navController.navigate(Screen.Training.route)
                        },
                        onViewAnalytics = {
                            navController.navigate(Screen.Analytics.route)
                        },
                        onViewHighlights = {
                            navController.navigate(Screen.Highlights.route)
                        }
                    )
                }

                composable(Screen.Training.route) {
                    TrainingScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Highlights.route) {
                    HighlightsScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

