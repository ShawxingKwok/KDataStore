package pers.shawxingkwok.kdatastore.demo.settings

import kotlinx.serialization.Serializable
import pers.shawxingkwok.kdatastore.KDataStore

@Serializable
data class Location(val latitude: Float, val longitude: Float)

@Serializable
data class User(val id: Long, val password: String, val location: Location)

object Settings : KDataStore("settings") {
    // `@JvmStatic` is needless if never called from Java.
    @JvmStatic
    val isDarkMode by storeNullable<Boolean>()
}