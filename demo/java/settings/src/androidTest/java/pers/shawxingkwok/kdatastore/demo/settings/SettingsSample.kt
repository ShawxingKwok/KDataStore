@file:Suppress("unused")

package pers.shawxingkwok.kdatastore.demo.settings

import kotlinx.serialization.Serializable
import pers.shawxingkwok.kdatastore.KDataStore
import java.math.BigDecimal

@Serializable
data class Location(val latitude: Float, val longitude: Float)

@Serializable
data class User(val id: Long, val password: String, val location: Location)

// `@JvmStatic` is needless if never called from Java.
object Settings : KDataStore("settings") {
    // store Serializable with default
    @JvmStatic
    val isVip by store(false)

    @JvmStatic
    val users by store(emptyList<User>())

    // default in `storeNullable` is limited to null.
    @JvmStatic
    val user by storeNullable<User>()

    // store customed data with conversion
    @JvmStatic
    val price by store<BigDecimal, Double>(
        default = BigDecimal(0.0),
        convert = { it.toDouble() },
        recover = { BigDecimal(it) }
    )

    @JvmStatic
    val _price by storeNullable<BigDecimal, Double>(
        convert = { it.toDouble() },
        recover = { BigDecimal(it) }
    )
}