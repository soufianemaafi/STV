package com.example.stv.navigation

/**
 * Définition des routes de navigation Compose.
 *
 * Chaque écran interne de l'app (sauf PlayerActivity qui reste une Activity séparée
 * pour PiP, singleTask et deep links) est représenté par une route.
 *
 * Home → écran d'accueil (drawer, boutons)
 * Videos → liste des flux sauvegardés
 * AddVideo → formulaire ajout d'un flux
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Videos : Screen("videos")
    data object AddVideo : Screen("add_video")
}

