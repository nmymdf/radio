package com.archiekuo.radio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.archiekuo.radio.R

object RC {
    val Navy0 = Color(0xFF070C26)
    val Navy1 = Color(0xFF0B1336)
    val Navy2 = Color(0xFF0E1844)
    val Glow = Color(0xFF26357A)
    val Card = Color(0xFF141C4A)
    val CardTop = Color(0xFF1C2558)
    val Gold = Color(0xFFF2C86A)
    val GoldLight = Color(0xFFFFE8AD)
    val GoldDark = Color(0xFFC78C2E)
    val Sub = Color(0xFFB9C0DC)
    val Dim = Color(0xFF8A93B8)
    val Green = Color(0xFF4ADE80)
    val GreenText = Color(0xFF5EE08A)
    val Red = Color(0xFFFF8A8A)
    val Line = Color(0x14FFFFFF)
}

val SerifTitle = FontFamily(Font(R.font.noto_serif_tc_black, FontWeight.Black))

val GoldTextBrush = Brush.verticalGradient(listOf(Color(0xFFFFF3C8), RC.Gold, Color(0xFFC58C2C)))
val GoldButtonBrush = Brush.verticalGradient(listOf(RC.GoldLight, Color(0xFFE8B65C), RC.GoldDark))
val CardBrush = Brush.verticalGradient(listOf(Color(0xF2202A60), Color(0xF2141C48)))

@Composable
fun RadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RC.Gold,
            onPrimary = Color(0xFF1A1405),
            background = RC.Navy0,
            onBackground = Color.White,
            surface = RC.Card,
            onSurface = Color.White,
            surfaceVariant = RC.CardTop,
            onSurfaceVariant = RC.Sub,
        ),
        content = content,
    )
}
