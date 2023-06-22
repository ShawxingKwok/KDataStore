package pers.shawxingkwok.kdatastore.compose.settings

import pers.shawxingkwok.kdatastore.KDataStore
import java.io.Serializable
import java.math.BigDecimal

// object Settings : KDataStore("settings") {
//     val isDarkMode by storeNullableBool()
//
//     val price by storeJavaSerializable(BigDecimal(0))
// }

data class User(val id: Long, val password: String) : Serializable

object Settings: KDataStore("settings"){
    val users by storeJavaSerializable(arrayListOf<User>())
}