package com.ps.padhaiplus.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle(val id: String, val emoji: String, val displayName: String, val primaryColor: Color) {
    INDIGO("indigo", "🪻", "Lavender", AccentLavender),
    OCEAN("ocean", "🌊", "Ocean", AccentOcean),
    EMERALD("emerald", "🌲", "Sage", AccentSage),
    ROSE("rose", "🌹", "Rose", AccentRose),
    AMBER("amber", "🌅", "Amber", AccentAmber);

    companion object {
        fun fromId(id: String?): AppThemeStyle {
            return entries.find { it.id == id } ?: INDIGO
        }
    }
}

enum class ThemeMode(val id: String, val displayName: String, val emoji: String) {
    LIGHT("light", "Light", "☀️"),
    DARK("dark", "Dark", "🌙"),
    SYSTEM("system", "System", "⚡");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.find { it.id == id } ?: SYSTEM
        }
    }
}

data class AppThemeColors(
    val primary: Color = AccentLavender,
    val primaryContainer: Color = IndigoContainerLight,
    val background: Color = BackgroundLight,
    val surface: Color = SurfaceLight,
    val surfaceVariant: Color = Color(0xFFF1F5F9),
    val cardSurface: Color = SurfaceLight,
    val softContainer: Color = MintPastelContainer,
    val mintContainer: Color = MintPastelContainer,
    val mintText: Color = MintPastelText,
    val peachContainer: Color = PeachPastelContainer,
    val trackBg: Color = Color(0xFFE2E8F0),
    val textPrimary: Color = Color(0xFF1E1B4B),
    val textSecondary: Color = Color(0xFF64748B),
    val borderAccent: Color = Color(0xFFE2E8F0),
    val navSelectedBg: Color = MintPastelContainer,
    val navSelectedText: Color = MintPastelText,
    val blobTopRight: Color = BlobPeachLight,
    val blobBottomLeft: Color = BlobBlueLight
)

val LocalAppTheme = compositionLocalOf { AppThemeColors() }

val SubjectSwatchColors = listOf(
    Color(0xFFE0F2FE), // Soft Blue / Math
    Color(0xFFDCF2E9), // Soft Mint / Physics
    Color(0xFFFFF2E6), // Soft Peach / English
    Color(0xFFF3E8FF), // Soft Purple / Computer
    Color(0xFFFCE7F3), // Soft Pink / Chemistry
    Color(0xFFFEF3C7)  // Soft Yellow / History
)

val SubjectSwatchIcons = listOf("📐", "🧪", "📖", "💻", "🎨", "🌍", "📝", "⚡")

@Composable
fun PastelBlobBackground(content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-right soft peach blob
            drawArc(
                color = theme.blobTopRight,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(width * 0.45f, -height * 0.12f),
                size = Size(width * 0.7f, width * 0.7f)
            )

            // Bottom-left soft blue circle blob
            drawCircle(
                color = theme.blobBottomLeft,
                radius = width * 0.42f,
                center = Offset(-width * 0.05f, height * 0.62f)
            )
        }

        content()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    secondary = EmeraldGreen,
    background = BackgroundDark,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    primaryContainer = IndigoContainerLight,
    secondary = EmeraldGreen,
    background = BackgroundLight,
    surface = SurfaceLight
)

@Composable
fun PadhaiPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    selectedTheme: AppThemeStyle = AppThemeStyle.INDIGO,
    content: @Composable () -> Unit
) {
    val actualDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }

    val primaryColor = selectedTheme.primaryColor

    val customColors = if (actualDarkTheme) {
        AppThemeColors(
            primary = primaryColor,
            primaryContainer = IndigoContainerDark,
            background = BackgroundDark,
            surface = SurfaceDark,
            surfaceVariant = Color(0xFF262438),
            cardSurface = SurfaceDark,
            softContainer = Color(0xFF232D3B),
            mintContainer = Color(0xFF1E3A34),
            mintText = Color(0xFFA8E6CF),
            peachContainer = Color(0xFF382A25),
            trackBg = Color(0xFF334155),
            textPrimary = Color.White,
            textSecondary = Color(0xFF94A3B8),
            borderAccent = Color(0xFF334155),
            navSelectedBg = Color(0xFF1E3A34),
            navSelectedText = Color(0xFFA8E6CF),
            blobTopRight = BlobPeachDark,
            blobBottomLeft = BlobBlueDark
        )
    } else {
        AppThemeColors(
            primary = primaryColor,
            primaryContainer = IndigoContainerLight,
            background = BackgroundLight,
            surface = SurfaceLight,
            surfaceVariant = Color(0xFFF1F5F9),
            cardSurface = SurfaceLight,
            softContainer = MintPastelContainer,
            mintContainer = MintPastelContainer,
            mintText = MintPastelText,
            peachContainer = PeachPastelContainer,
            trackBg = Color(0xFFF1F5F9),
            textPrimary = Color(0xFF1E1B4B),
            textSecondary = Color(0xFF64748B),
            borderAccent = Color(0xFFE2E8F0),
            navSelectedBg = MintPastelContainer,
            navSelectedText = MintPastelText,
            blobTopRight = BlobPeachLight,
            blobBottomLeft = BlobBlueLight
        )
    }

    val colorScheme = if (actualDarkTheme) DarkColorScheme.copy(primary = primaryColor) else LightColorScheme.copy(primary = primaryColor)

    CompositionLocalProvider(LocalAppTheme provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
