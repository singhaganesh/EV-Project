package com.ganesh.ev.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ClayLightColorScheme =
        lightColorScheme(
                primary = ClayPrimary,
                onPrimary = ClayOnPrimary,
                primaryContainer = ClayPrimaryContainer,
                onPrimaryContainer = ClayOnPrimaryContainer,
                secondary = ClaySecondary,
                onSecondary = ClayOnSecondary,
                secondaryContainer = ClaySecondaryContainer,
                onSecondaryContainer = ClayOnSecondaryContainer,
                tertiary = ClayTertiary,
                onTertiary = ClayOnTertiary,
                tertiaryContainer = ClayTertiaryContainer,
                onTertiaryContainer = ClayOnTertiaryContainer,
                background = ClayBackground,
                onBackground = ClayOnBackground,
                surface = ClaySurface,
                onSurface = ClayOnSurface,
                surfaceVariant = ClaySurfaceVariant,
                onSurfaceVariant = ClayOnSurfaceVariant,
                outline = ClayOutline,
                outlineVariant = ClayOutlineVariant,
                error = ClayError,
                onError = ClayOnPrimary,
                errorContainer = ClaySlotMaintenance,
                onErrorContainer = ClayOnSecondaryContainer
        )

private val ClayDarkColorScheme =
        darkColorScheme(
                primary = ClayDarkPrimary,
                onPrimary = ClayDarkOnPrimary,
                primaryContainer = ClayDarkPrimaryContainer,
                onPrimaryContainer = ClayDarkOnPrimaryContainer,
                secondary = ClayDarkSecondary,
                onSecondary = ClayDarkOnSecondary,
                secondaryContainer = ClayDarkSecondaryContainer,
                onSecondaryContainer = ClayDarkOnSecondaryContainer,
                tertiary = ClayDarkTertiary,
                onTertiary = ClayDarkOnTertiary,
                tertiaryContainer = ClayDarkTertiaryContainer,
                onTertiaryContainer = ClayDarkOnTertiaryContainer,
                background = ClayDarkBackground,
                onBackground = ClayDarkOnBackground,
                surface = ClayDarkSurface,
                onSurface = ClayDarkOnSurface,
                surfaceVariant = ClayDarkSurfaceVariant,
                onSurfaceVariant = ClayDarkOnSurfaceVariant,
                outline = ClayDarkOutline,
                outlineVariant = ClayDarkOutlineVariant,
                error = ClayError,
                onError = ClayOnPrimary,
                errorContainer = ClaySlotMaintenance,
                onErrorContainer = ClayOnSecondaryContainer
        )

@Composable
fun EvTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) ClayDarkColorScheme else ClayLightColorScheme

    MaterialTheme(
            colorScheme = colorScheme,
            shapes = ClayShapes,
            typography = ClayTypography,
            content = content
    )
}
