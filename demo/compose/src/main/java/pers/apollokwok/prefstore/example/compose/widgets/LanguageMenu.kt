package pers.apollokwok.prefstore.example.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pers.apollokwok.prefstore.compose.collectAsDefaultState
import pers.apollokwok.prefstore.example.compose.data.ds.Language
import pers.apollokwok.prefstore.example.compose.data.ds.Settings

@Composable
fun LanguageMenu(){
    var expanded by remember { mutableStateOf(false) }
    val savedLanguage = Settings.language.collectAsDefaultState().value

    Box {
        Text(
            text = "Language: $savedLanguage",
            modifier = Modifier.clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Language.values().forEach { language ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        Settings.language.toss { language }
                    },
                ) {
                    Text(text = "$language")
                }
            }
        }
    }
}