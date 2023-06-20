package pers.shawxingkwok.kdatastore.compose.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.compose.settings.Settings

private val map = mapOf(null to "Follow System", true to "Dark", false to "Light")

@Composable
fun DarkModeSelector() {
    val isDark = Settings.isDarkMode.collectAsState()

    Column {
        map.forEach { (option, text) ->
            Row {
                RadioButton(
                    selected = (option == isDark.value), // observe
                    onClick = { Settings.isDarkMode.value = option } // update
                )

                Text(
                    text = text,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}