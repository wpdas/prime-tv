package com.wpdas.primetv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wpdas.primetv.ui.screens.home.HomeScreen
import com.wpdas.primetv.ui.screens.placeholder.PlaceholderScreen
import com.wpdas.primetv.ui.screens.setup.SetupScreen

@Composable
fun PrimeNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenLive = { navController.navigate(LiveRoute) },
                onOpenDetails = { navController.navigate(DetailsRoute(it)) },
                onPlay = { id, episode -> navController.navigate(PlayerRoute(id, episode)) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SearchRoute> { PlaceholderScreen("screen.search", phase = 3, onBack = navController::popBackStack) }
        composable<DetailsRoute> {
            val route = it.toRoute<DetailsRoute>()
            PlaceholderScreen("screen.details", phase = 3, subtitle = route.itemId, onBack = navController::popBackStack)
        }
        composable<LiveRoute> { PlaceholderScreen("screen.live", phase = 3, onBack = navController::popBackStack) }
        composable<PlayerRoute> {
            val route = it.toRoute<PlayerRoute>()
            PlaceholderScreen("screen.player", phase = 4, subtitle = listOfNotNull(route.itemId, route.episodeLabel).joinToString(" · "), onBack = navController::popBackStack)
        }
        composable<AppMenuRoute> { PlaceholderScreen("screen.app_menu", phase = 7, onBack = navController::popBackStack) }
        composable<SettingsRoute> { PlaceholderScreen("screen.settings", phase = 6, onBack = navController::popBackStack) }
        composable<SetupRoute> {
            SetupScreen(
                onStartWatching = {
                    navController.navigate(HomeRoute) { popUpTo(SetupRoute) { inclusive = true } }
                },
                onReceiveLink = { navController.navigate(ReceiveLinkRoute) },
            )
        }
        composable<ReceiveLinkRoute> { PlaceholderScreen("screen.receive_link", phase = 6, onBack = navController::popBackStack) }
    }
}
