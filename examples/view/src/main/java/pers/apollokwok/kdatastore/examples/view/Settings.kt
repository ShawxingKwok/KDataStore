package pers.apollokwok.kdatastore.examples.view

import androidx.appcompat.app.AppCompatDelegate
import kotlinx.serialization.Serializable
import pers.shawxingkwok.kdatastore.KDataStore

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

object Settings : KDataStore(){
    val darkMode by enum(DarkMode.FOLLOW_SYSTEM)
}

enum class DarkMode(val text: String, val corresponding: Int){
    YES("Dark", AppCompatDelegate.MODE_NIGHT_YES),
    NO("Light", AppCompatDelegate.MODE_NIGHT_NO),
    FOLLOW_SYSTEM("Follow system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}