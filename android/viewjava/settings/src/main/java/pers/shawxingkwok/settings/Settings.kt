package pers.shawxingkwok.settings

import kotlinx.serialization.Serializable
import pers.apollokwok.kdatastore.KDataStore
import pers.apollokwok.kdatastore.asMutableLiveData

object Settings : KDataStore("settings") {
    @Serializable
    data class Dice(
        val diceCount: Int = 2,
        val sideCount: Int = 6,
        val uniqueRollsOnly: Boolean = false,
    )

    @JvmStatic
    val dice by store(Dice()).asMutableLiveData()

    @JvmStatic
    val isDarkMode by storeNullable<Boolean>().asMutableLiveData()
}