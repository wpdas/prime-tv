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
import com.wpdas.primetv.ui.screens.football.FootballScreen
import com.wpdas.primetv.ui.screens.live.LiveScreen
import com.wpdas.primetv.ui.screens.receivelink.ReceiveLinkScreen
import com.wpdas.primetv.ui.screens.settings.SettingsScreen
import com.wpdas.primetv.ui.screens.update.UpdateScreen
import com.wpdas.primetv.ui.screens.player.PlayerScreen
import com.wpdas.primetv.ui.screens.search.SearchScreen
import com.wpdas.primetv.ui.screens.setup.SetupScreen

/**
 * [resumeChannelId]: canal ao vivo que estava em tela cheia quando o app foi fechado; abre o
 * player por cima da Início assim que chegar (uma vez, [onResumeConsumed] marca como feito).
 */
@Composable
fun PrimeNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
    resumeChannelId: String? = null,
    onResumeConsumed: () -> Unit = {},
) {
    fun openItem(item: MediaItem) {
        if (item.type == MediaType.CHANNEL) navController.navigate(PlayerRoute(item.id))
        else navController.navigate(DetailsRoute(item.id))
    }

    androidx.compose.runtime.LaunchedEffect(resumeChannelId) {
        if (resumeChannelId != null) {
            onResumeConsumed()
            navController.navigate(PlayerRoute(resumeChannelId))
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenLive = { navController.navigate(LiveRoute) },
                onOpenFootball = { navController.navigate(FootballRoute) },
                onOpenDetails = { navController.navigate(DetailsRoute(it)) },
                onPlay = { id, episode -> navController.navigate(PlayerRoute(id, episode)) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onSeeAll = { row ->
                    row.section?.let { navController.navigate(CategoryRoute(it.name, row.categoryId, row.title)) }
                },
                onOpenUpdate = { navController.navigate(UpdateRoute) },
            )
        }
        composable<LiveRoute> {
            LiveScreen(
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.LIVE -> Unit
                        HomeTab.FOOTBALL -> navController.navigate(FootballRoute) { popUpTo<LiveRoute> { inclusive = true } }
                        else -> navController.navigate(HomeRoute(tab.name)) { popUpTo<HomeRoute> { inclusive = true } }
                    }
                },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onPlay = { channel -> navController.navigate(PlayerRoute(channel.id)) },
            )
        }
        composable<FootballRoute> {
            FootballScreen(
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.FOOTBALL -> Unit
                        HomeTab.LIVE -> navController.navigate(LiveRoute) { popUpTo<FootballRoute> { inclusive = true } }
                        else -> navController.navigate(HomeRoute(tab.name)) { popUpTo<HomeRoute> { inclusive = true } }
                    }
                },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenAppMenu = { navController.navigate(AppMenuRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onPlayChannel = { id -> navController.navigate(PlayerRoute(id)) },
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
                onChangePlaylist = { navController.navigate(SetupRoute()) },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<AboutRoute> { AboutScreen(onBack = navController::popBackStack) }
        composable<UpdateRoute> { UpdateScreen(onClose = navController::popBackStack) }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = navController::popBackStack,
                onChangePlaylist = { navController.navigate(SetupRoute()) },
                onReceiveLink = { navController.navigate(ReceiveLinkRoute) },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<SetupRoute> {
            SetupScreen(
                onStartWatching = {
                    // Lista nova: a Início recomeça do zero, sem telas antigas na pilha.
                    navController.navigate(HomeRoute()) { popUpTo(navController.graph.id) { inclusive = true } }
                },
                onReceiveLink = { navController.navigate(ReceiveLinkRoute) },
                onOpenUpdate = { navController.navigate(UpdateRoute) },
            )
        }
        composable<ReceiveLinkRoute> {
            ReceiveLinkScreen(
                onBack = navController::popBackStack,
                onUseLink = { link -> navController.navigate(SetupRoute(prefill = link)) { popUpTo<ReceiveLinkRoute> { inclusive = true } } },
            )
        }
    }
}
