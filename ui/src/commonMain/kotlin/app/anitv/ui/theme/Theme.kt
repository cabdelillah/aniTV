package app.anitv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AniTvColors {
    val Accent = Color(0xFFFF4D6D)
    val Background = Color(0xFF0E0E12)
    val Surface = Color(0xFF17171D)
    val SurfaceHigh = Color(0xFF22222A)
    val OnSurfaceMuted = Color(0xFFA0A0AE)
    val Score = Color(0xFFFFC857)
}

private val scheme = darkColorScheme(
    primary = AniTvColors.Accent,
    onPrimary = Color.White,
    secondary = AniTvColors.Score,
    background = AniTvColors.Background,
    onBackground = Color(0xFFF2F2F5),
    surface = AniTvColors.Surface,
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = AniTvColors.SurfaceHigh,
    onSurfaceVariant = AniTvColors.OnSurfaceMuted,
    surfaceContainer = AniTvColors.Surface,
    surfaceContainerHigh = AniTvColors.SurfaceHigh,
)

@Composable
fun AniTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Parses AniList's `#rrggbb` colors. */
fun parseHexColor(hex: String?): Color? {
    val value = hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}
