package pers.apollokwok.prefstore.example.compose.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pers.apollokwok.prefstore.compose.collectAsDefaultState
import pers.apollokwok.prefstore.example.compose.data.ds.Settings

@Composable
fun LocationText(){
    val location = Settings.location.collectAsDefaultState().value

    Row {
        Column {
            Spacer(modifier = Modifier.height(15.dp))
            Text("Location:")
        }
        Spacer(modifier = Modifier.width(10.dp))

        val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)

        TextField(
            modifier = Modifier.width(90.dp),
            label = { Text("latitude") },
            value = "${location.lat}",
            keyboardOptions = keyboardOptions,
            onValueChange = { lat: String ->
                Settings.location.toss{
                    it.copy(lat = lat.toDouble())
                }
            },
        )
        Spacer(modifier = Modifier.width(20.dp))

        TextField(
            modifier = Modifier.width(90.dp),
            label = { Text("longitude") },
            value = "${location.lng}",
            keyboardOptions = keyboardOptions,
            onValueChange = { lng: String ->
                Settings.location.toss{
                    it.copy(lng = lng.toDouble())
                }
            },
        )
    }
}