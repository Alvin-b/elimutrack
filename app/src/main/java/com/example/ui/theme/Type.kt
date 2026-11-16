package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val FamiljenGroteskFamily = FontFamily(
  Font(R.font.familjen_grotesk, FontWeight.Medium),
  Font(R.font.familjen_grotesk, FontWeight.SemiBold),
  Font(R.font.familjen_grotesk, FontWeight.Bold),
)

val HankenGroteskFamily = FontFamily(
  Font(R.font.hanken_grotesk, FontWeight.Normal),
  Font(R.font.hanken_grotesk, FontWeight.Medium),
  Font(R.font.hanken_grotesk, FontWeight.SemiBold),
  Font(R.font.hanken_grotesk, FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val AppTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = FamiljenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 27.sp,
    lineHeight = 31.sp,
    letterSpacing = (-0.4).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = FamiljenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.3).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = FamiljenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 19.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.2).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = FamiljenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 23.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = FamiljenGroteskFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 21.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 19.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 21.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 19.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp,
    lineHeight = 17.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = HankenGroteskFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
)
