@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING",
    "MemberVisibilityCanBePrivate"
)

package pers.shawxingkwok.kdatastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

enum class Language {
    ENGLISH, GERMAN, SPANISH
}

@Serializable
data class Location(val lat: Double, val lng: Double)

// encryption = Encryption.AES(
// ivBytes =
// listOf(
// 0x16, 0x09, 0xc0, 0x4d,
// 0x4a, 0x09, 0xd2, 0x46,
// 0x71, 0xcc, 0x32, 0xb7,
// 0xd2, 0x91, 0x8a, 0x9c,
// )
// .map { it.toByte() }
// .toByteArray(),
// salt = "salt",
// password = "demo",
// ),
// defaultEncrypted = true,

@RunWith(AndroidJUnit4::class)
class Settings : KDataStore(
    cypher = Cypher.AES("fa", "Fdq", ivBytes = (1..16).map { it.toByte() }.toByteArray())
) {
    val bool by bool(false)
    val int by int(1)
    val long by long(1)
    val float by float(1.0f)
    val double by double(1.0)
    val string by string("1")
    val enum by enum(Language.ENGLISH)
    val ktSerializable by ktSerializable(Location(38.2, 55.3))
    val javaSerializable by javaSerializable(linkedSetOf(1))
    val any by any(
        default = 1,
        convert = { it.toString() },
        recover = { it.toInt() }
    )

    @OptIn(DelicateApi::class)
    @Test
    fun start(): Unit = runBlocking {
        launch {
            val flow1 = combine(int, long) { a, b -> "$a $b" }
            val flow2 = combine(float, double, bool) { a, b, c -> "$a $b $c" }
            val flow3 = combine(string, any, enum, ktSerializable, javaSerializable)
            { a, b, c, d, e -> "$a $b $c $d $e" }

            combine(flow1, flow2, flow3) { a, b, c -> "$a $b $c" }
                .collect { MLog(it) }
        }

        launch {
            emitTest()
        }
        launch {
           tossTest()
        }
    }

    suspend fun emitTest() {
        MLog("emit")

        while (true) {
            delay(500)
            int.emit { it + 1 }
            long.emit { it + 1 }
            float.emit { it + 1 }
            double.emit { it + 1 }
            bool.emit { !it }
            string.emit { (it.toLong() + 1).toString() }
            any.emit { it + 1 }
            enum.emit {
                val i = Language.values().indexOf(it)
                Language.values().getOrElse(i + 1) { Language.values().first() }
            }
            ktSerializable.emit { it.copy(lat = it.lat + 1, lng = it.lng + 1) }
            javaSerializable.emit { linkedSetOf(it.last() + 1) }
        }
    }

    suspend fun tossTest() {
        MLog("toss")

        while (true) {
            delay(500)
            int.cast { it + 1 }
            long.cast { it + 1 }
            float.cast { it + 1 }
            double.cast { it + 1 }
            bool.cast { !it }

            string.cast { (it.toLong() + 1).toString() }
            any.cast { it + 1 }
            enum.cast {
                val i = Language.values().indexOf(it)
                Language.values().getOrElse(i + 1) { Language.values().first() }
            }
            ktSerializable.cast { it.copy(lat = it.lat + 1, lng = it.lng + 1) }
            javaSerializable.cast { linkedSetOf(it.last() + 1) }
        }
    }
}