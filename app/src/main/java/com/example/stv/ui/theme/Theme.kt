package com.example.stv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============ DARK THEME - NETFLIX STYLE ============
private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,                    // #E50914 - Rouge Netflix
    onPrimary = WhitePrimary,                // Blanc sur rouge
    primaryContainer = RedDark,              // #B2070E - Conteneurs rouges
    onPrimaryContainer = WhitePrimary,       // Blanc sur conteneur rouge

    secondary = AccentCyan,                  // #00D9FF - Cyan accent
    onSecondary = BlackDark,                 // Noir sur cyan
    secondaryContainer = Color(0xFF005F73), // Conteneur cyan foncé
    onSecondaryContainer = WhitePrimary,     // Blanc sur conteneur cyan

    tertiary = AccentYellow,                 // #FFB81E - Jaune
    onTertiary = BlackDark,                  // Noir sur jaune

    error = RedLight,                        // #FF3D3D - Erreurs
    onError = WhitePrimary,                  // Blanc sur erreur

    background = BlackDark,                  // #0D0D0D - Fond très noir
    onBackground = WhitePrimary,             // Blanc sur fond

    surface = BlackCard,                     // #1A1A1A - Surface cards
    onSurface = WhitePrimary,                // Blanc sur surface
    surfaceVariant = GrayMuted,              // #808080 - Variante surface
    onSurfaceVariant = GrayLight,            // Gris clair sur surface

    outline = GrayLight,                     // #B3B3B3 - Bordures
    outlineVariant = GrayMuted               // #808080 - Bordures alt
)

// ============ LIGHT THEME (Fallback) ============
private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = AccentCyan,
    tertiary = AccentYellow
)

@Composable
fun STVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Désactiver les couleurs dynamiques pour cohérence
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Barre de statut (en haut) - Rouge YouTube
            window.statusBarColor = RedPrimary.toArgb()  // #C41E3A
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = false

            // Appliquer aussi au décor pour assurer la cohérence
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility

            // Barre de navigation (en bas) - Noir foncé (comme le fond)
            window.navigationBarColor = BlackVeryDark.toArgb()  // #121212
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

