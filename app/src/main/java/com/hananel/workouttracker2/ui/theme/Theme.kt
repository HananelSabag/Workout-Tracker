package com.hananel.workouttracker2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFEDD5),   // Orange 100
    onPrimaryContainer   = Color(0xFF431407),   // Deep orange-brown

    secondary            = Secondary,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD1FAE5),   // Emerald 100
    onSecondaryContainer = Color(0xFF064E3B),   // Deep emerald

    tertiary             = Color(0xFF3B82F6),   // Blue 500 — timer / rest state
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDBEAFE),   // Blue 100
    onTertiaryContainer  = Color(0xFF1E3A8A),   // Deep blue

    background           = Background,
    onBackground         = TextPrimary,
    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = Color(0xFFF1F5F9),   // Slate 100
    onSurfaceVariant     = TextSecondary,
    outline              = Color(0xFFCBD5E1),   // Slate 300
    outlineVariant       = Color(0xFFE2E8F0),   // Slate 200

    error                = Error,
    onError              = Color.White,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),

    inverseSurface       = Color(0xFF161B22),
    inverseOnSurface     = Color(0xFFF0F6FC),
    inversePrimary       = PrimaryDark,
    scrim                = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryDark,
    onPrimary            = Color(0xFF431407),   // Dark text sits on the orange button in dark mode
    primaryContainer     = Color(0xFF7C2D12),   // Orange 900
    onPrimaryContainer   = Color(0xFFFFEDD5),   // Light orange text

    secondary            = SecondaryDark,
    onSecondary          = Color(0xFF064E3B),
    secondaryContainer   = Color(0xFF065F46),   // Emerald 800
    onSecondaryContainer = Color(0xFFD1FAE5),

    tertiary             = Color(0xFF60A5FA),   // Blue 400
    onTertiary           = Color(0xFF1E3A8A),
    tertiaryContainer    = Color(0xFF1D4ED8),   // Blue 700
    onTertiaryContainer  = Color(0xFFDBEAFE),

    background           = BackgroundDark,
    onBackground         = TextPrimaryDark,
    surface              = SurfaceDark,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = Color(0xFF21262D),
    onSurfaceVariant     = TextSecondaryDark,
    outline              = Color(0xFF30363D),
    outlineVariant       = Color(0xFF21262D),

    error                = ErrorDark,
    onError              = Color(0xFF7F1D1D),
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = ErrorDark,

    inverseSurface       = Color(0xFFF1F5F9),
    inverseOnSurface     = Color(0xFF0F172A),
    inversePrimary       = Primary,
    scrim                = Color(0xFF000000),
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
