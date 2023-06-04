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

@RunWith(AndroidJUnit4::class)
class SettingsTest : KDataStore(
    encryption = Encryption.AES(
        ivBytes =
            listOf(
                0x16, 0x09, 0xc0, 0x4d,
                0x4a, 0x09, 0xd2, 0x46,
                0x71, 0xcc, 0x32, 0xb7,
                0xd2, 0x91, 0x8a, 0x9c,
            )
            .map { it.toByte() }
            .toByteArray(),
        salt = "salt",
        password = "demo",
    ),
    defaultEncrypted = true,
){
    val byte by byte(1)
    val short by short(1)
    val int by int(1)
    val long by long(1)
    val float by float(1.0f)
    val double by double(1.0)
    val char by char('1')
    val boolean by bool(false)
    val string by string("1")
    val enum by enum(Language.ENGLISH)
    val ktSerializable by ktSerializable(Location(38.2, 55.3))
    val javaSerializable by javaSerializable(linkedSetOf(1))
    val any by any(
        default = 1,
        convert = { it.toString() },
        recover = { it.toInt() }
    )

    @Test
    fun foo(): Unit = runBlocking{
        launch {
            val flow1 = combine(byte, short, int, long) { a, b, c, d -> "$a $b $c $d" }
            val flow2 = combine(float, double, boolean, char) { a, b, c, d -> "$a $b $c $d" }
            val flow3 = combine(string, any, enum, ktSerializable, javaSerializable)
            { a, b, c, d, e -> "$a $b $c $d $e" }

            combine(flow1, flow2, flow3) { a, b, c -> "$a $b $c" }
            .collect { MLog(it) }
        }

        launch {
            emitTest()
        }
        launch {
//            tossTest()
        }
    }

    suspend fun emitTest() {
        MLog("emit")

        while (true) {
            delay(500)
            byte.emit { (it + 1).toByte() }
            short.emit { (it + 1).toShort() }
            int.emit { it + 1 }
            long.emit { it + 1 }
            float.emit { it + 1 }
            double.emit { it + 1 }
            boolean.emit { !it }
            char.emit { it + 1 }

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
            byte.toss { (it + 1).toByte() }
            short.toss { (it + 1).toShort() }
            int.toss { it + 1 }
            long.toss { it + 1 }
            float.toss { it + 1 }
            double.toss { it + 1 }
            boolean.toss { !it }
            char.toss { it + 1 }

            string.toss { (it.toLong() + 1).toString() }
            any.toss { it + 1 }
            enum.toss {
                val i = Language.values().indexOf(it)
                Language.values().getOrElse(i + 1) { Language.values().first() }
            }
            ktSerializable.toss { it.copy(lat = it.lat + 1, lng = it.lng + 1) }
            javaSerializable.toss { linkedSetOf(it.last() + 1) }
        }
    }
}