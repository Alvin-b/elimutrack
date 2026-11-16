package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ElimuExtraColors(
  val navy: Color,
  val highlighter: Color,
  val highlighterInk: Color,
  val penRed: Color,
  val ruleLine: Color,
  val ee: Color,
  val me: Color,
  val ae: Color,
  val be: Color,
  val line: Color,
  val muted: Color,
)

val LocalElimuColors = staticCompositionLocalOf {
  ElimuExtraColors(
    navy = BookNavyLight,
    highlighter = HighlighterYellow,
    highlighterInk = HighlighterInk,
    penRed = PenRed,
    ruleLine = BookRuleLight,
    ee = CbcEeLight,
    me = CbcMeLight,
    ae = CbcAeLight,
    be = CbcBeLight,
    line = BookLineLight,
    muted = BookMutedLight,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = BookNavyLight,
  onPrimary = Color.White,
  primaryContainer = HighlighterYellow,
  onPrimaryContainer = HighlighterInk,
  secondary = CbcMeLight,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0ECFF),
  onSecondaryContainer = BookNavyLight,
  tertiary = CbcEeLight,
  onTertiary = Color.White,
  background = BookBgLight,
  onBackground = BookInkLight,
  surface = BookCardLight,
  onSurface = BookInkLight,
  surfaceVariant = Color(0xFFE3E9F4),
  onSurfaceVariant = BookMutedLight,
  outline = BookLineLight,
  error = CbcBeLight,
  onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
  primary = BookNavyDark,
  onPrimary = Color.White,
  primaryContainer = HighlighterYellow,
  onPrimaryContainer = HighlighterInk,
  secondary = CbcMeDark,
  onSecondary = BookBgDark,
  secondaryContainer = Color(0xFF1D3354),
  onSecondaryContainer = BookInkDark,
  tertiary = CbcEeDark,
  onTertiary = BookBgDark,
  background = BookBgDark,
  onBackground = BookInkDark,
  surface = BookCardDark,
  onSurface = BookInkDark,
  surfaceVariant = Color(0xFF182942),
  onSurfaceVariant = BookMutedDark,
  outline = BookLineDark,
  error = CbcBeDark,
  onError = BookBgDark,
)

val ElimuShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
)

@Composable
fun ElimuTrackTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extra = if (darkTheme) {
    ElimuExtraColors(
      navy = BookNavyDark,
      highlighter = HighlighterYellow,
      highlighterInk = HighlighterInk,
      penRed = CbcBeDark,
      ruleLine = BookRuleDark,
      ee = CbcEeDark,
      me = CbcMeDark,
      ae = CbcAeDark,
      be = CbcBeDark,
      line = BookLineDark,
      muted = BookMutedDark,
    )
  } else {
    ElimuExtraColors(
      navy = BookNavyLight,
      highlighter = HighlighterYellow,
      highlighterInk = HighlighterInk,
      penRed = PenRed,
      ruleLine = BookRuleLight,
      ee = CbcEeLight,
      me = CbcMeLight,
      ae = CbcAeLight,
      be = CbcBeLight,
      line = BookLineLight,
      muted = BookMutedLight,
    )
  }

  CompositionLocalProvider(LocalElimuColors provides extra) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = ElimuShapes,
      content = content,
    )
  }
}
