package pers.shawxingkwok.kdatastore.compose.ui.theme

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import pers.shawxingkwok.kdatastore.compose.settings.Settings
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun MyTheme(content: @Composable () -> Unit) {
    // observe
    val isDarkMode = Settings.isDarkMode.collectAsState()

    MaterialTheme(
        colors = if (isDarkMode.value ?: isSystemInDarkTheme()) DarkColorPalette else LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}