package com.example.stv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.stv.VideoItem
import com.example.stv.VideoListViewModel
import com.example.stv.ui.screens.AddVideoScreen
import com.example.stv.ui.screens.HomeScreen
import com.example.stv.ui.screens.VideoListScreen

/**
 * Graphe de navigation Compose — point unique de configuration.
 *
 * 3 destinations internes :
 *   Home     → écran d'accueil (drawer, boutons +, vidéos)
 *   Videos   → liste des flux sauvegardés
 *   AddVideo → formulaire ajout flux
 *
 * PlayerActivity reste une Activity séparée (PiP, singleTask, deep links).
 *
 * @param navController      Contrôleur de navigation partagé
 * @param videoListViewModel ViewModel partagé entre Videos et AddVideo
 */
@Composable
fun STVNavGraph(
    navController: NavHostController,
    videoListViewModel: VideoListViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        // ✅ Transitions animées fluides (slide + fade)
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
        // ──── Écran d'accueil ────
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

        // ──── Liste des vidéos ────
        composable(Screen.Videos.route) {
            // ✅ Rafraîchir les vidéos à chaque affichage (équivalent onResume)
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

        // ──── Formulaire ajout ────
        composable(Screen.AddVideo.route) {
            AddVideoScreen(
                onSave = { title, url ->
                    // ✅ ViewModel partagé — plus de communication par Intent
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

