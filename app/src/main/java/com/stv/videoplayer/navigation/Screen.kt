package com.stv.videoplayer.navigation

/**
 * DÃ©finition des routes de navigation Compose.
 *
 * Chaque Ã©cran interne de l'app (sauf PlayerActivity qui reste une Activity sÃ©parÃ©e
 * pour PiP, singleTask et deep links) est reprÃ©sentÃ© par une route.
 *
 * Home â†’ Ã©cran d'accueil (drawer, boutons)
 * Videos â†’ liste des flux sauvegardÃ©s
 * AddVideo â†’ formulaire ajout d'un flux
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Videos : Screen("videos")
    data object AddVideo : Screen("add_video")
}


