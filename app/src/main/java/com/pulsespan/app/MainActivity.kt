package com.pulsespan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pulsespan.app.ui.PulseSpanViewModel
import com.pulsespan.app.ui.screens.CoachSettingsScreen
import com.pulsespan.app.ui.screens.SleepRecoveryScreen
import com.pulsespan.app.ui.screens.StrainScreen
import com.pulsespan.app.ui.screens.TodayScreen
import com.pulsespan.app.ui.theme.PulseColors
import com.pulsespan.app.ui.theme.PulseSpanTheme

private enum class PulseTab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Default.Favorite),
    RECOVERY("recovery", "Recovery", Icons.Default.Bedtime),
    STRAIN("strain", "Strain", Icons.Default.Bolt),
    COACH("coach", "Coach", Icons.Default.AutoAwesome)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PulseSpanApplication).container

        setContent {
            PulseSpanTheme {
                val vm: PulseSpanViewModel = viewModel(factory = PulseSpanViewModel.factory(container))
                PulseSpanApp(viewModel = vm)
            }
        }
    }
}

@Composable
private fun PulseSpanApp(viewModel: PulseSpanViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: PulseTab.TODAY.route

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val requestPermissions: () -> Unit = {
        permissionLauncher.launch(viewModel.requiredPermissions)
    }

    Scaffold(
        containerColor = PulseColors.Background,
        bottomBar = {
            NavigationBar(containerColor = PulseColors.Surface) {
                PulseTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PulseColors.RecoveryGreen,
                            selectedTextColor = PulseColors.RecoveryGreen,
                            unselectedIconColor = PulseColors.TextSecondary,
                            unselectedTextColor = PulseColors.TextSecondary,
                            indicatorColor = PulseColors.SurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PulseTab.TODAY.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(PulseTab.TODAY.route) {
                TodayScreen(
                    uiState = uiState,
                    onRequestPermissions = requestPermissions,
                    onRefresh = viewModel::refreshData,
                    onOpenCoach = { navController.navigate(PulseTab.COACH.route) }
                )
            }
            composable(PulseTab.RECOVERY.route) {
                SleepRecoveryScreen(uiState = uiState)
            }
            composable(PulseTab.STRAIN.route) {
                StrainScreen(uiState = uiState)
            }
            composable(PulseTab.COACH.route) {
                CoachSettingsScreen(
                    uiState = uiState,
                    onSendQuestion = viewModel::sendCoachQuestion,
                    onToggleDemoMode = viewModel::toggleDemoMode,
                    onRequestPermissions = requestPermissions
                )
            }
        }
    }
}
