package com.synex.mirarecorder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.synex.mirarecorder.ui.recordings.RecordingsScreen
import com.synex.mirarecorder.ui.settings.SettingsScreen
import com.synex.mirarecorder.ui.targets.TargetsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Targets : Screen("targets", "Targets", Icons.Default.Person)
    data object Recordings : Screen("recordings", "Recordings", Icons.Default.FiberManualRecord)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val screens = listOf(Screen.Targets, Screen.Recordings, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Targets.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Screen.Targets.route) { TargetsScreen() }
            composable(Screen.Recordings.route) { RecordingsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
