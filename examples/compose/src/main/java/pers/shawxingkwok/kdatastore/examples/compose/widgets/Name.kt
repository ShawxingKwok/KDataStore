package pers.shawxingkwok.kdatastore.examples.compose.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.compose.collectAsDefaultState
import pers.shawxingkwok.kdatastore.examples.compose.Settings

@Composable
fun Name() {
    Row {
        Column {
            Spacer(modifier = Modifier.height(15.dp))
            Text("Name")
        }

        Spacer(modifier = Modifier.width(10.dp))

        TextField(
            modifier = Modifier.width(120.dp),
            value = Settings.name.collectAsDefaultState().value,
            onValueChange = Settings.name::toss,
        )
    }
}