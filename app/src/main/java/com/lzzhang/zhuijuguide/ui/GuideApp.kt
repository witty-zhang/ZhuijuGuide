package com.lzzhang.zhuijuguide.ui

import android.util.Base64
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lzzhang.zhuijuguide.ui.screens.FavoritesScreen
import com.lzzhang.zhuijuguide.ui.screens.HomeScreen
import com.lzzhang.zhuijuguide.ui.screens.PlayerScreen
import com.lzzhang.zhuijuguide.ui.screens.SettingsScreen
import com.lzzhang.zhuijuguide.ui.screens.WorkDetailScreen

@Composable
fun GuideApp(viewModel: GuideViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val openWork: (com.lzzhang.zhuijuguide.model.WorkSummary) -> Unit = { work ->
        viewModel.openWork(work)
        navController.navigate("work")
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in TOP_LEVEL_ROUTES) {
                NavigationBar {
                    NavigationDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { rootPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = rootPadding.calculateBottomPadding()),
        ) {
            composable("home") {
                HomeScreen(
                    state = state,
                    onQueryChange = viewModel::setQuery,
                    onReload = viewModel::reloadRecommendations,
                    onWorkClick = openWork,
                    onFavoriteClick = viewModel::toggleFavorite,
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    state = state,
                    onWorkClick = openWork,
                    onFavoriteClick = viewModel::toggleFavorite,
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onAddFeed = viewModel::addFeed,
                    onRemoveFeed = viewModel::removeFeed,
                )
            }
            composable("work") {
                WorkDetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onPlay = { episode ->
                        val title = listOfNotNull(state.selectedWork?.title, episode.title)
                            .joinToString(" · ")
                        navController.navigate(
                            "player/${encodeRouteValue(episode.url)}/${encodeRouteValue(title)}",
                        )
                    },
                    onFavoriteClick = viewModel::toggleFavorite,
                )
            }
            composable(
                route = "player/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { entry ->
                PlayerScreen(
                    title = decodeRouteValue(entry.arguments?.getString("title").orEmpty()),
                    url = decodeRouteValue(entry.arguments?.getString("url").orEmpty()),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private enum class NavigationDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HOME("home", "影视", Icons.Rounded.Home),
    FAVORITES("favorites", "收藏", Icons.Rounded.Favorite),
    SETTINGS("settings", "设置", Icons.Rounded.Settings),
}

private val TOP_LEVEL_ROUTES = NavigationDestination.entries.map(NavigationDestination::route).toSet()

private fun encodeRouteValue(value: String): String {
    return Base64.encodeToString(
        value.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
}

private fun decodeRouteValue(value: String): String {
    return runCatching {
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .toString(Charsets.UTF_8)
    }.getOrDefault(value)
}
