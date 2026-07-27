package com.umityasincoban.nefesizi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.umityasincoban.nefesizi.feature.analytics.AnalyticsScreen
import com.umityasincoban.nefesizi.feature.health.HealthScreen
import com.umityasincoban.nefesizi.feature.onboarding.OnboardingScreen
import com.umityasincoban.nefesizi.feature.records.RecordsScreen
import com.umityasincoban.nefesizi.feature.settings.SettingsScreen
import com.umityasincoban.nefesizi.feature.today.TodayScreen

@Composable
fun NefesIziRoot(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        AppState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        AppState.Onboarding -> OnboardingScreen()
        AppState.Ready -> MainNavigation()
    }
}

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    MainDestination("today", "Bugün", Icons.Outlined.Home),
    MainDestination("records", "Kayıtlar", Icons.Outlined.History),
    MainDestination("health", "Sağlık", Icons.Outlined.FavoriteBorder),
    MainDestination("analytics", "Analiz", Icons.Outlined.BarChart),
    MainDestination("settings", "Ayarlar", Icons.Outlined.Settings),
)

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(padding),
        ) {
            composable("today") { TodayScreen(snackbarHostState) }
            composable("records") { RecordsScreen(snackbarHostState) }
            composable("health") { HealthScreen(snackbarHostState) }
            composable("analytics") { AnalyticsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
