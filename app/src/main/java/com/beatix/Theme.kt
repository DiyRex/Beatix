package com.beatix

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pioneer-style dark console with amber accents.
val Bg = Color(0xFF0B0B0D)
val Panel = Color(0xFF18181C)
val PanelDark = Color(0xFF111114)
val Amber = Color(0xFFF5A623)
val AmberDim = Color(0xFF4A3A14)
val PadIdle = Color(0xFF26262B)
val TextCol = Color(0xFFE6E6E6)
val GrayBtn = Color(0xFFB9B9BE)

@Composable
fun BeatixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Amber,
            onPrimary = Color.Black,
            background = Bg,
            onBackground = TextCol,
            surface = Panel,
            onSurface = TextCol,
        ),
        content = content,
    )
}
