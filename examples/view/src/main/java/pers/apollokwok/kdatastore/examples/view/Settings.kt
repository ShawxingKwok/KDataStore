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
    val isDarkMode by bool(false)
}