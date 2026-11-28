package com.ps.padhaiplus.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeStyle(val id: String, val emoji: String, val displayName: String) {
    INDIGO("indigo", "🌙", "Indigo Night"),
    EMERALD("emerald", "🌲", "Emerald Forest"),
    AMBER("amber", "🌅", "Amber Sunset");

    companion object {
        fun fromId(id: String?): AppThemeStyle {
            return entries.find { it.id == id } ?: INDIGO
        }
    }
}

data class AppThemeColors(
    val primary: Color = IndigoPrimary,
    val primaryContainer: Color = IndigoContainerDark,
    val background: Color = BackgroundDark,
    val surface: Color = SurfaceDark,
    val surfaceVariant: Color = Color(0xFF334155),
    val cardSurface: Color = SurfaceDark,
    val softContainer: Color = Color(0xFF1E293B),
    val trackBg: Color = Color(0xFF475569),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color.Gray,
    val borderAccent: Color = Color(0xFF475569),
    val navSelectedBg: Color = Color(0xFF1E293B)
)

val LocalAppTheme = compositionLocalOf { AppThemeColors() }

val SubjectSwatchColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Red
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899)  // Pink
)

val SubjectSwatchIcons = listOf("📚", "📐", "🔬", "💻", "🎨", "🌍", "📝", "⚡")

@Composable
fun PastelBlobBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    secondary = EmeraldGreen,
    tertiary = AmberGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    primaryContainer = IndigoContainerLight,
    secondary = EmeraldGreen,
    tertiary = AmberGold,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9)
)

@Composable
fun PadhaiPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    selectedTheme: AppThemeStyle = AppThemeStyle.INDIGO,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
