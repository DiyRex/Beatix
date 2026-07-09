package com.beatix

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pioneer-style dark console with amber accents + soft gradients.
val Bg = Color(0xFF09090B)
val Panel = Color(0xFF161619)
val PanelHi = Color(0xFF202027)
val PanelDark = Color(0xFF0C0C0F)
val PadTop = Color(0xFF2C2C34)
val PadBot = Color(0xFF191920)
val Amber = Color(0xFFF5A623)
val AmberHi = Color(0xFFFFC558)
val AmberLo = Color(0xFFC9810F)
val Edge = Color(0xFF34343C)
val TextCol = Color(0xFFEDEDF0)
val TextDim = Color(0xFF8C8C94)
val GrayBtn = Color(0xFFC2C2C8)
val Cyan = Color(0xFF35C4E0)   // headphone-cue accent

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
