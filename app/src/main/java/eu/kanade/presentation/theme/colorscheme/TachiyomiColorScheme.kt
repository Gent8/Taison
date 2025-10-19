package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for the Sunrise default theme
 * Centered around the accent color #EAB710
 */
internal object TachiyomiColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFFD166),
        onPrimary = Color(0xFF402C00),
        primaryContainer = Color(0xFF5E4200),
        onPrimaryContainer = Color(0xFFFFE19C),
        inversePrimary = Color(0xFFEAB710),
        secondary = Color(0xFFD6B35A), // Unread badge
        onSecondary = Color(0xFF382300), // Unread badge text
        secondaryContainer = Color(0xFF533700), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFFFDDAF), // Navigation bar selector icon
        tertiary = Color(0xFF4CD8DD), // Downloaded badge
        onTertiary = Color(0xFF00363A), // Downloaded badge text
        tertiaryContainer = Color(0xFF004F54),
        onTertiaryContainer = Color(0xFF70F6FB),
        background = Color(0xFF1F1A12),
        onBackground = Color(0xFFEBE0CF),
        surface = Color(0xFF1F1A12),
        onSurface = Color(0xFFEBE0CF),
        surfaceVariant = Color(0xFF504533), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFD5C2A6),
        surfaceTint = Color(0xFFFFD166),
        inverseSurface = Color(0xFFFBEFDB),
        inverseOnSurface = Color(0xFF1F1A12),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF9D8F75),
        outlineVariant = Color(0xFF504533),
        surfaceContainerLowest = Color(0xFF171107),
        surfaceContainerLow = Color(0xFF1F180D),
        surfaceContainer = Color(0xFF271F13), // Navigation bar background
        surfaceContainerHigh = Color(0xFF32281C),
        surfaceContainerHighest = Color(0xFF3D3226),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFEAB710),
        onPrimary = Color(0xFF402D00),
        primaryContainer = Color(0xFFFFE08A),
        onPrimaryContainer = Color(0xFF2A1800),
        inversePrimary = Color(0xFF815600),
        secondary = Color(0xFF8C6A00), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFFFFDC86), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF241800), // Navigation bar selector icon
        tertiary = Color(0xFF006874), // Downloaded badge
        onTertiary = Color(0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(0xFF7CE7F1),
        onTertiaryContainer = Color(0xFF001F23),
        background = Color(0xFFFFFBF3),
        onBackground = Color(0xFF1F1A12),
        surface = Color(0xFFFFFBF3),
        onSurface = Color(0xFF1F1A12),
        surfaceVariant = Color(0xFFF1DFC3), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF504533),
        surfaceTint = Color(0xFFEAB710),
        inverseSurface = Color(0xFF352F25),
        inverseOnSurface = Color(0xFFFBEFDB),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF84745B),
        outlineVariant = Color(0xFFD5C2A6),
        surfaceContainerLowest = Color(0xFFFFF4E1),
        surfaceContainerLow = Color(0xFFFFEBD0),
        surfaceContainer = Color(0xFFFFE1BD), // Navigation bar background
        surfaceContainerHigh = Color(0xFFFFD7A8),
        surfaceContainerHighest = Color(0xFFFFCD94),
    )
}
