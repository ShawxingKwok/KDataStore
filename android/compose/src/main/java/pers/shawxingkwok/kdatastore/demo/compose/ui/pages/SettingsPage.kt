package pers.shawxingkwok.kdatastore.demo.compose.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.demo.compose.ui.widgets.DarkModeSelector

@Composable
fun SettingsPage() {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ){
        Spacer(modifier = Modifier.fillMaxWidth().height(300.dp)) // other widgets

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ){
            DarkModeSelector()
        }
    }
}