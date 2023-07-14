package pers.shawxingkwok.settings

import kotlinx.serialization.Serializable
import pers.apollokwok.kdatastore.KDataStore

// Settings for traditional java is inside dir 'android.viewjava'.
object Settings : KDataStore("settings") {
    @Serializable
    data class Dice(
        val diceCount: Int = 2,
        val sideCount: Int = 6,
        val uniqueRollsOnly: Boolean = false,
    )

    val dice by store(Dice())
    val isDarkMode by storeNullable<Boolean>()
}