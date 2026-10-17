package com.example.digitallibrary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DevotionalLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = DeepMaroon,
    onSecondary = Color.White,
    secondaryContainer = MaroonContainer,
    onSecondaryContainer = OnMaroonContainer,
    tertiary = MutedGold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = ParchmentBackground,
    onBackground = TextDevotionalPrimary,
    surface = ParchmentSurface,
    onSurface = TextDevotionalPrimary,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = TextDevotionalSecondary,
    outline = ParchmentDivider,
    outlineVariant = ParchmentBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

private val DevotionalDarkColorScheme = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Color.White,
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = DeepMaroonLight,
    onSecondary = Color.White,
    secondaryContainer = DeepMaroonDark,
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = MutedGoldLight,
    onTertiary = Color.Black,
    tertiaryContainer = MutedGoldDark,
    onTertiaryContainer = Color(0xFFFFF2BF),
    background = SanctumBackgroundDark,
    onBackground = TextDarkDevotionalPrimary,
    surface = SanctumSurfaceDark,
    onSurface = TextDarkDevotionalPrimary,
    surfaceVariant = SanctumSurfaceVariantDark,
    onSurfaceVariant = TextDarkDevotionalSecondary,
    outline = SanctumDividerDark,
    outlineVariant = SanctumBorderDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun DigitalLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false to preserve authentic devotional palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DevotionalDarkColorScheme
        else -> DevotionalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
