package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AmoledColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = AmoledBackground,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = EmberAccent,
    onSecondary = AmoledBackground,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = DarkBackground,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = EmberAccent,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = AmoledTextPrimary,
    surface = DarkSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = AmoledTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SaffronDark,
    onPrimary = LightSurface,
    primaryContainer = SaffronLight,
    onPrimaryContainer = SaffronDark,
    secondary = EmberAccent,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun KothaPadTheme(
    themeSetting: String = "AMOLED",
    customPrimary: Color = SaffronPrimary,
    customBackground: Color = AmoledBackground,
    customSurface: Color = AmoledSurface,
    customText: Color = AmoledTextPrimary,
    customSecondary: Color = EmberAccent,
    cornerRadiusDp: Int = 12,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isCustom = themeSetting.uppercase() == "CUSTOM"
    val bgLuminance = customBackground.red * 0.299f + customBackground.green * 0.587f + customBackground.blue * 0.114f
    val isDark = when {
        isCustom -> bgLuminance < 0.5f
        themeSetting.uppercase() == "AMOLED" -> true
        themeSetting.uppercase() == "DARK" -> true
        themeSetting.uppercase() == "LIGHT" -> false
        else -> isSystemDark
    }

    val customColorScheme = if (bgLuminance < 0.5f) {
        darkColorScheme(
            primary = customPrimary,
            onPrimary = if (customPrimary.red * 0.299f + customPrimary.green * 0.587f + customPrimary.blue * 0.114f > 0.5f) Color.Black else Color.White,
            primaryContainer = customPrimary.copy(alpha = 0.25f),
            onPrimaryContainer = customPrimary,
            secondary = customSecondary,
            onSecondary = Color.White,
            background = customBackground,
            onBackground = customText,
            surface = customSurface,
            onSurface = customText,
            surfaceVariant = customSurface.copy(alpha = 0.85f),
            onSurfaceVariant = customText.copy(alpha = 0.7f),
            outline = customPrimary.copy(alpha = 0.35f)
        )
    } else {
        lightColorScheme(
            primary = customPrimary,
            onPrimary = Color.White,
            primaryContainer = customPrimary.copy(alpha = 0.18f),
            onPrimaryContainer = customPrimary,
            secondary = customSecondary,
            onSecondary = Color.White,
            background = customBackground,
            onBackground = customText,
            surface = customSurface,
            onSurface = customText,
            surfaceVariant = customSurface.copy(alpha = 0.9f),
            onSurfaceVariant = customText.copy(alpha = 0.7f),
            outline = customPrimary.copy(alpha = 0.3f)
        )
    }

    val colorScheme = when {
        isCustom -> customColorScheme
        themeSetting.uppercase() == "AMOLED" -> AmoledColorScheme
        themeSetting.uppercase() == "DARK" -> DarkColorScheme
        themeSetting.uppercase() == "LIGHT" -> LightColorScheme
        else -> if (isSystemDark) AmoledColorScheme else LightColorScheme
    }

    val shapes = Shapes(
        small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(2).dp),
        medium = RoundedCornerShape(cornerRadiusDp.dp),
        large = RoundedCornerShape((cornerRadiusDp * 1.5f).toInt().dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = AppTypography,
        content = content
    )
}
