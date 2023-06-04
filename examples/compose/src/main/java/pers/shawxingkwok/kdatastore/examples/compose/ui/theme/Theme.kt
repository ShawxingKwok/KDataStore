package pers.shawxingkwok.kdatastore.examples.compose.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import pers.shawxingkwok.kdatastore.compose.collectAsDefaultStateWithLifecycle
import pers.shawxingkwok.kdatastore.examples.compose.data.Settings

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
fun KDataStoreTheme(content: @Composable () -> Unit) {
    val isDarkMode = Settings.isDarkMode.collectAsDefaultStateWithLifecycle()

    MaterialTheme(
        colors = if (isDarkMode.value) DarkColorPalette else LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}