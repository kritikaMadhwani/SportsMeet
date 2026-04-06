package com.madhwanikritika.sportsmeet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.madhwanikritika.sportsmeet.ui.theme.CreamWarm
import com.madhwanikritika.sportsmeet.ui.theme.Ivory
import com.madhwanikritika.sportsmeet.ui.theme.MintPale
import com.madhwanikritika.sportsmeet.ui.theme.PrimaryGreen
import com.madhwanikritika.sportsmeet.ui.theme.PrimaryGreenDark
import com.madhwanikritika.sportsmeet.ui.theme.SageWash
import com.madhwanikritika.sportsmeet.ui.theme.WhitePure

@Composable
fun AuthGradientBackground(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val brush = if (dark) {
        Brush.verticalGradient(
            colors = listOf(
                PrimaryGreenDark.copy(alpha = 0.55f),
                Color(0xFF0D1520),
                Color(0xFF0A1218)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                PrimaryGreen.copy(alpha = 0.12f),
                MintPale.copy(alpha = 0.9f),
                SageWash,
                CreamWarm,
                Ivory,
                WhitePure
            )
        )
    }
    Box(
        modifier
            .fillMaxSize()
            .background(brush)
    )
}
