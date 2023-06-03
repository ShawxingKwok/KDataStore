package pers.shawxingkwok.kdatastore.examples.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.examples.compose.widgets.LocationText
import pers.shawxingkwok.kdatastore.examples.compose.widgets.Name

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
                    Name()
                    Spacer(modifier = Modifier.fillMaxWidth().height(20.dp))
                    LocationText()
                    Spacer(modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }
    }
}