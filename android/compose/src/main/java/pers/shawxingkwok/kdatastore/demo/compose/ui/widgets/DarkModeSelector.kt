package pers.shawxingkwok.kdatastore.demo.compose.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pers.shawxingkwok.settings.Settings

private val arr = arrayOf(null to "Follow system", true to "Dark", false to "Light")

@Composable
fun DarkModeSelector() {
    // set state
    val isDark = Settings.isDarkMode.collectAsState()

    Column {
        arr.forEach { (option, text) ->
            Row {
                RadioButton(
                    // link `isDarkMode` to radioButton display
                    selected = option == isDark.value,
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