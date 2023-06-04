package pers.shawxingkwok.kdatastore.examples.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pers.shawxingkwok.kdatastore.KDataStore
import pers.shawxingkwok.kdatastore.examples.compose.ui.theme.KDataStoreTheme
import pers.shawxingkwok.kdatastore.compose.collectAsDefaultStateWithLifecycle
import pers.shawxingkwok.kdatastore.examples.compose.ui.pages.SettingsPage
import pers.shawxingkwok.kdatastore.examples.compose.ui.widgets.DarkModeSwitch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KDataStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    SettingsPage()
                }
            }
        }
    }
}