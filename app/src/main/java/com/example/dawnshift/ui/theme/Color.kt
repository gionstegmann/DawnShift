package com.example.dawnshift.ui.theme

import androidx.compose.ui.graphics.Color

// Sunrise / Dawn Theme Palette
val SunriseOrange = Color(0xFFFF8C42) // Warm sun
val MorningYellow = Color(0xFFFFD166) // Soft morning light
val SkyBlue = Color(0xFF4D9DE0)       // Early morning sky
val DeepDawnBlue = Color(0xFF1A1B41)  // Pre-dawn sky (Dark mode background)
val CloudWhite = Color(0xFFF7F9F9)    // Clouds
val MistGray = Color(0xFFE1E5F2)      // Morning mist

// Accent colors
val SuccessGreen = Color(0xFF06D6A0) // Completed days

// Material 3 mappings (approximate for now, can be refined)
val PrimaryLight = SunriseOrange
val OnPrimaryLight = Color.White
val PrimaryContainerLight = MorningYellow.copy(alpha = 0.3f)
val OnPrimaryContainerLight = Color(0xFF4A2500)

val SecondaryLight = SkyBlue
val OnSecondaryLight = Color.White
val SecondaryContainerLight = SkyBlue.copy(alpha = 0.2f)
val OnSecondaryContainerLight = Color(0xFF003355)

val TertiaryLight = SuccessGreen
val OnTertiaryLight = Color.White
val TertiaryContainerLight = SuccessGreen.copy(alpha = 0.2f)
val OnTertiaryContainerLight = Color(0xFF003828)

val BackgroundLight = CloudWhite
val OnBackgroundLight = DeepDawnBlue
val SurfaceLight = Color.White
val OnSurfaceLight = DeepDawnBlue

// Dark Mode
val PrimaryDark = SunriseOrange
val OnPrimaryDark = Color.Black
val PrimaryContainerDark = SunriseOrange.copy(alpha = 0.3f)
val OnPrimaryContainerDark = MorningYellow

val SecondaryDark = SkyBlue.copy(alpha = 0.8f)
val OnSecondaryDark = Color.Black
val SecondaryContainerDark = SkyBlue.copy(alpha = 0.3f)
val OnSecondaryContainerDark = Color.White

val TertiaryDark = SuccessGreen.copy(alpha = 0.8f)
val OnTertiaryDark = Color.Black
val TertiaryContainerDark = SuccessGreen.copy(alpha = 0.3f)
val OnTertiaryContainerDark = Color.White

val BackgroundDark = DeepDawnBlue
val OnBackgroundDark = CloudWhite
val SurfaceDark = DeepDawnBlue.copy(alpha = 0.9f) // Slightly lighter than background
val OnSurfaceDark = CloudWhite

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)