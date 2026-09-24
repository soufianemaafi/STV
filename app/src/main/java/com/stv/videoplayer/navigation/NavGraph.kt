package com.stv.videoplayer.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stv.videoplayer.core.domain.model.VideoItem
import com.stv.videoplayer.features.home.presentation.VideoListViewModel
import com.stv.videoplayer.ui.screens.AddVideoScreen
import com.stv.videoplayer.ui.screens.HomeScreen
import com.stv.videoplayer.ui.screens.VideoListScreen

/**
 * Graphe de navigation Compose â€” point unique de configuration.
 *
 * 3 destinations internes :
 *   Home     â†’ Ã©cran d'accueil (drawer, boutons +, vidÃ©os)
 *   Videos   â†’ liste des flux sauvegardÃ©s
 *   AddVideo â†’ formulaire ajout flux
 *
 * PlayerActivity reste une Activity sÃ©parÃ©e (PiP, singleTask, deep links).
 *
 * @param navController      ContrÃ´leur de navigation partagÃ©
 * @param videoListViewModel ViewModel partagÃ© entre Videos et AddVideo
 */
@Composable
fun STVNavGraph(
    navController: NavHostController,
    videoListViewModel: VideoListViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        // âœ… Transitions animÃ©es fluides (slide + fade)
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // â”€â”€â”€â”€ Ã‰cran d'accueil â”€â”€â”€â”€
        composable(Screen.Home.route) {
            HomeScreen(
                onAddVideoClick = {
                    navController.navigate(Screen.AddVideo.route) {
                        launchSingleTop = true
                    }
                },
                onVideosClick = {
                    navController.navigate(Screen.Videos.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // â”€â”€â”€â”€ Liste des vidÃ©os â”€â”€â”€â”€
        composable(Screen.Videos.route) {
            // âœ… RafraÃ®chir les vidÃ©os Ã  chaque affichage (Ã©quivalent onResume)
            videoListViewModel.refreshVideos()

            VideoListScreen(
                viewModel = videoListViewModel,
                onAddClick = {
                    navController.navigate(Screen.AddVideo.route) {
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // â”€â”€â”€â”€ Formulaire ajout â”€â”€â”€â”€
        composable(Screen.AddVideo.route) {
            AddVideoScreen(
                onSave = { title, url ->
                    // âœ… ViewModel partagÃ© â€” plus de communication par Intent
                    videoListViewModel.addVideo(VideoItem(title = title, url = url))
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}


