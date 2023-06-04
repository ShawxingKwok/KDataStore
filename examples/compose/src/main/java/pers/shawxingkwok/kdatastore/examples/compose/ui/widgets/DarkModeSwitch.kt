package pers.shawxingkwok.kdatastore.examples.compose.ui.widgets

import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import pers.shawxingkwok.kdatastore.compose.collectAsDefaultStateWithLifecycle
import pers.shawxingkwok.kdatastore.examples.compose.data.Settings

@Composable
fun DarkModeSwitch(){
    Text("Dark mode")

    val isDarkMode = Settings.isDarkMode.collectAsDefaultStateWithLifecycle()

    Switch(
        checked = isDarkMode.value,
        onCheckedChange = { Settings.isDarkMode.toss(it) },
        // or onCheckedChange = Settings.isDarkMode::toss
    )
}