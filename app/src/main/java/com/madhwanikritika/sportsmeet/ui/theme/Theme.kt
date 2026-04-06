package com.madhwanikritika.sportsmeet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CB4E8),
    onPrimary = NavyDeep,
    primaryContainer = Color(0xFF2A4060),
    onPrimaryContainer = BlueMist,
    secondary = BrandGold,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0xFF5D4A00),
    onSecondaryContainer = GoldPale,
    tertiary = TanSoft,
    onTertiary = NavyDeep,
    tertiaryContainer = BrownRich,
    onTertiaryContainer = CreamWarm,
    background = Color(0xFF0D1520),
    onBackground = Pearl,
    surface = Color(0xFF151D2A),
    onSurface = Pearl,
    surfaceVariant = Color(0xFF2A3444),
    onSurfaceVariant = TanSoft,
    outline = GreenMist,
    outlineVariant = BrownMuted,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = Color(0xFF0D1520),
    surfaceBright = Color(0xFF2A3444),
    surfaceContainerLowest = Color(0xFF0A1018),
    surfaceContainerLow = Color(0xFF121A28),
    surfaceContainer = Color(0xFF1A2333),
    surfaceContainerHigh = Color(0xFF242E3F),
    surfaceContainerHighest = Color(0xFF2F3A4C),
    inverseSurface = CreamWarm,
    inverseOnSurface = NavyDeep,
    inversePrimary = PrimaryNavyDark,
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = WhitePure,
    primaryContainer = MintSoft,
    onPrimaryContainer = NavyDeep,
    secondary = BrandGold,
    onSecondary = NavyDeep,
    secondaryContainer = GoldPale,
    onSecondaryContainer = Color(0xFF5D4A00),
    tertiary = BrownRich,
    onTertiary = WhitePure,
    tertiaryContainer = BeigeWarm,
    onTertiaryContainer = NavyDeep,
    background = CreamWarm,
    onBackground = NavyDeep,
    surface = WhitePure,
    onSurface = NavyDeep,
    surfaceVariant = MintPale,
    onSurfaceVariant = BrownMuted,
    outline = GreenMist,
    outlineVariant = BrownOutline,
    error = Color(0xFFBA1A1A),
    onError = WhitePure,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceDim = Pearl,
    surfaceBright = Ivory,
    surfaceContainerLowest = WhitePure,
    surfaceContainerLow = OffWhite,
    surfaceContainer = SageWash,
    surfaceContainerHigh = MintPale,
    surfaceContainerHighest = BeigeWarm,
    inverseSurface = NavyDeep,
    inverseOnSurface = CreamWarm,
    inversePrimary = SageDeep,
    scrim = Color(0xFF000000)
)

@Composable
fun SportsMeetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
