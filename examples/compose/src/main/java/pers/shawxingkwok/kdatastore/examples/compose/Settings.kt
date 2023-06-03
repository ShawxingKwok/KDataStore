package pers.shawxingkwok.kdatastore.examples.compose

import kotlinx.serialization.Serializable
import pers.shawxingkwok.kdatastore.KDataStore

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

object Settings : KDataStore(){
    val name by string("Shawxing")
    val location by ktSerializable(Location(0.0, 0.0))
}