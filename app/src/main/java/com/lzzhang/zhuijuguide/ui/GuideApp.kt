package com.lzzhang.zhuijuguide.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lzzhang.zhuijuguide.model.ResourceType
import com.lzzhang.zhuijuguide.ui.components.ResourceDetailSheet
import com.lzzhang.zhuijuguide.ui.screens.FavoritesScreen
import com.lzzhang.zhuijuguide.ui.screens.HomeScreen
import com.lzzhang.zhuijuguide.ui.screens.LiveChannelsScreen
import com.lzzhang.zhuijuguide.ui.screens.PlayerScreen
import com.lzzhang.zhuijuguide.ui.screens.SettingsScreen
import com.lzzhang.zhuijuguide.ui.screens.TvHomeScreen
import com.lzzhang.zhuijuguide.ui.screens.WebScreen
import kotlinx.coroutines.launch

@Composable
fun GuideApp(viewModel: GuideViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isTv = LocalIsTv.current
    var selectedResourceId by remember { mutableStateOf<String?>(null) }
    val selectedResource = selectedResourceId?.let { id ->
        state.allResources.firstOrNull { it.id == id }
    }
    val showNavigation = currentRoute in TOP_LEVEL_ROUTES

    val openExternal: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("没有可打开此链接的应用") }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val onResourceClick: (com.lzzhang.zhuijuguide.model.MediaResource) -> Unit = {
        selectedResourceId = it.id
    }

    if (isTv) {
        // 电视端：左侧竖向导航栏。底部导航在 16:9 大屏上既浪费高度，
        // 焦点路径也容易和网格混在一起。
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showNavigation) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationDestination.entries.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTopLevel(navController, currentRoute, destination) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
                NavContent(
                    navController = navController,
                    state = state,
                    viewModel = viewModel,
                    isTv = true,
                    onResourceClick = onResourceClick,
                    modifier = Modifier.weight(1f),
                )
            }
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNavigation) {
                    NavigationBar {
                        NavigationDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTopLevel(navController, currentRoute, destination) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { rootPadding ->
            NavContent(
                navController = navController,
                state = state,
                viewModel = viewModel,
                isTv = false,
                onResourceClick = onResourceClick,
                modifier = Modifier.padding(bottom = rootPadding.calculateBottomPadding()),
            )
        }
    }

    selectedResource?.let { resource ->
        ResourceDetailSheet(
            resource = resource,
            isFavorite = resource.id in state.favorites,
            onDismiss = { selectedResourceId = null },
            onFavorite = { viewModel.toggleFavorite(resource.id) },
            onOpen = {
                selectedResourceId = null
                when {
                    resource.type == ResourceType.PLAYLIST -> {
                        viewModel.openLiveSource(resource)
                        navController.navigate("live")
                    }
                    resource.isDirectMedia -> navController.navigate(
                        "player/${encodeRouteValue(resource.url)}/${encodeRouteValue(resource.title)}",
                    )
                    resource.type == ResourceType.WEB -> navController.navigate(
                        "web/${encodeRouteValue(resource.url)}/${encodeRouteValue(resource.title)}",
                    )
                    else -> openExternal(resource.url)
                }
            },
        )
    }
}

@Composable
private fun NavContent(
    navController: NavHostController,
    state: GuideUiState,
    viewModel: GuideViewModel,
    isTv: Boolean,
    onResourceClick: (com.lzzhang.zhuijuguide.model.MediaResource) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
    ) {
        composable("home") {
            if (isTv) {
                TvHomeScreen(
                    state = state,
                    onQueryChange = viewModel::setQuery,
                    onCategoryChange = viewModel::setCategory,
                    onCheck = { viewModel.checkAvailability(announce = true) },
                    onResourceClick = onResourceClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                )
            } else {
                HomeScreen(
                    state = state,
                    onQueryChange = viewModel::setQuery,
                    onCategoryChange = viewModel::setCategory,
                    onCheck = { viewModel.checkAvailability(announce = true) },
                    onResourceClick = onResourceClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                )
            }
        }
        composable("favorites") {
            FavoritesScreen(
                state = state,
                onResourceClick = onResourceClick,
                onFavoriteClick = viewModel::toggleFavorite,
            )
        }
        composable("settings") {
            SettingsScreen(
                state = state,
                onCheck = { viewModel.checkAvailability(announce = true) },
                onImportPlaylist = viewModel::importPlaylist,
                onPostMessage = viewModel::postMessage,
            )
        }
        composable("live") {
            LiveChannelsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReload = viewModel::reloadLiveSource,
                onQueryChange = viewModel::setLiveQuery,
                onChannelClick = { channel ->
                    navController.navigate(
                        "player/${encodeRouteValue(channel.url)}/${encodeRouteValue(channel.title)}",
                    )
                },
            )
        }
        composable(
            route = "web/{url}/{title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { entry ->
            WebScreen(
                title = decodeRouteValue(entry.arguments?.getString("title").orEmpty()),
                url = decodeRouteValue(entry.arguments?.getString("url").orEmpty()),
                onBack = { navController.popBackStack() },
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

private fun navigateTopLevel(
    navController: NavHostController,
    currentRoute: String?,
    destination: NavigationDestination,
) {
    if (currentRoute == destination.route) return
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private enum class NavigationDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HOME("home", "资源", Icons.Rounded.Home),
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
