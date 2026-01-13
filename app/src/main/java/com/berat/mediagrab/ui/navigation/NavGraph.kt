package com.berat.mediagrab.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.berat.mediagrab.ui.screens.DownloadsScreen
import com.berat.mediagrab.ui.screens.HomeScreen
import com.berat.mediagrab.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
}

private const val ANIMATION_DURATION = 300

@Composable
fun MediaGrabNavHost(navController: NavHostController) {
    NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(ANIMATION_DURATION)) +
                        slideInHorizontally(
                                animationSpec = tween(ANIMATION_DURATION),
                                initialOffsetX = { it / 4 }
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(ANIMATION_DURATION)) +
                        slideOutHorizontally(
                                animationSpec = tween(ANIMATION_DURATION),
                                targetOffsetX = { -it / 4 }
                        )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(ANIMATION_DURATION)) +
                        slideInHorizontally(
                                animationSpec = tween(ANIMATION_DURATION),
                                initialOffsetX = { -it / 4 }
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(ANIMATION_DURATION)) +
                        slideOutHorizontally(
                                animationSpec = tween(ANIMATION_DURATION),
                                targetOffsetX = { it / 4 }
                        )
            }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
