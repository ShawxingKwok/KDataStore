package pers.apollokwok.prefstore.example.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.apollokwok.prefstore.example.compose.widgets.LanguageMenu
import pers.apollokwok.prefstore.example.compose.widgets.LocationText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LanguageMenu()
                    Spacer(modifier = Modifier.fillMaxWidth().height(20.dp))
                    LocationText()
                    Spacer(modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }
    }
}