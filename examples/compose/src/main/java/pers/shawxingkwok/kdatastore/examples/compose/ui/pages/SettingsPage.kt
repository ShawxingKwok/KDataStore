package pers.shawxingkwok.kdatastore.examples.compose.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.examples.compose.ui.widgets.DarkModeSwitch

@Composable
fun SettingsPage() {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ){
        // other widgets
        Spacer(modifier = Modifier.fillMaxWidth().height(300.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ){
            DarkModeSwitch()
        }
    }
}