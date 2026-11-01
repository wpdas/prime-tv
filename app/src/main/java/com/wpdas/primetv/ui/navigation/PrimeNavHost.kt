package com.wpdas.primetv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.ui.screens.about.AboutScreen
import com.wpdas.primetv.ui.screens.appmenu.AppMenuScreen
import com.wpdas.primetv.ui.screens.category.CategoryScreen
import com.wpdas.primetv.ui.screens.details.DetailsScreen
import com.wpdas.primetv.ui.screens.home.HomeScreen
import com.wpdas.primetv.ui.screens.live.LiveScreen
import com.wpdas.primetv.ui.screens.placeholder.PlaceholderScreen
import com.wpdas.primetv.ui.screens.player.PlayerScreen
import com.wpdas.primetv.ui.screens.search.SearchScreen
import com.wpdas.primetv.ui.screens.setup.SetupScreen

@Composable
fun PrimeNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
) {
    fun openItem(item: MediaItem) {
        if (item.type == MediaType.CHANNEL) navController.navigate(PlayerRoute(item.id))
        else navController.navigate(DetailsRoute(item.id))
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenLive = { navController.navigate(LiveRoute) },
                onOpenDetails = { navController.navigate(DetailsRoute(it)) },
                onPlay = { id, episode -> navController.navigate(PlayerRoute(id, episode)) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onSeeAll = { row ->
                    row.section?.let { navController.navigate(CategoryRoute(it.name, row.categoryId, row.title)) }
                },
            )
        }
        composable<LiveRoute> {
            LiveScreen(
                onTabSelected = { tab ->
                    if (tab != HomeTab.LIVE) {
                        navController.navigate(HomeRoute(tab.name)) { popUpTo<HomeRoute> { inclusive = true } }
                    }
                },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onPlay = { channel -> navController.navigate(PlayerRoute(channel.id)) },
            )
        }
        composable<CategoryRoute> {
            CategoryScreen(onBack = navController::popBackStack, onOpenItem = ::openItem)
        }
        composable<DetailsRoute> {
            DetailsScreen(
                onBack = navController::popBackStack,
                onPlay = { id, episode -> navController.navigate(PlayerRoute(id, episode?.id)) },
                onOpenItem = ::openItem,
            )
        }
        composable<SearchRoute> {
            SearchScreen(onBack = navController::popBackStack, onOpenItem = ::openItem)
        }
        composable<PlayerRoute> { PlayerScreen(onBack = navController::popBackStack) }
        composable<AppMenuRoute> {
            AppMenuScreen(
                onCheckUpdate = { navController.navigate(UpdateRoute) },
                onSettings = { navController.navigate(SettingsRoute) },
                onChangePlaylist = { navController.navigate(SetupRoute) },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<AboutRoute> { AboutScreen(onBack = navController::popBackStack) }
        composable<UpdateRoute> { PlaceholderScreen("screen.update", phase = 7, onBack = navController::popBackStack) }
        composable<SettingsRoute> { PlaceholderScreen("screen.settings", phase = 6, onBack = navController::popBackStack) }
        composable<SetupRoute> {
            SetupScreen(
                onStartWatching = {
                    navController.navigate(HomeRoute()) { popUpTo<SetupRoute> { inclusive = true } }
                },
                onReceiveLink = { navController.navigate(ReceiveLinkRoute) },
            )
        }
        composable<ReceiveLinkRoute> { PlaceholderScreen("screen.receive_link", phase = 6, onBack = navController::popBackStack) }
    }
}
