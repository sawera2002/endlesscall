package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleDarkColorScheme =
  darkColorScheme(
    primary = PurpleVibrant,
    onPrimary = Color.White,
    primaryContainer = DeepPurpleCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = VioletElectric,
    onSecondary = Color.White,
    secondaryContainer = DeepPurpleBorder,
    onSecondaryContainer = TextSecondary,
    tertiary = MagentaNeon,
    onTertiary = Color.White,
    background = DeepPurpleDark,
    onBackground = TextPrimary,
    surface = DeepPurpleSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepPurpleCard,
    onSurfaceVariant = TextSecondary,
    outline = DeepPurpleBorder,
    error = CallHangupRed
  )

private val PurpleLightColorScheme =
  lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2E7FE),
    onPrimaryContainer = Color(0xFF3700B3),
    secondary = VioletElectric,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = MagentaNeon,
    background = Color(0xFFF9F5FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFD0BCFF)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek purple dark theme to showcase purple gradients
  dynamicColor: Boolean = false, // Keep purple gradient theme consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

