package pers.shawxingkwok.kdatastore

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.Serializable
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class SerializableTest {
    @OptIn(ExperimentalTime::class)
    @Test
    fun foo(){
        val a = A(1, 1.0, "fjrp", O("qp"))
        val _a = _A(1, 1.0, "fjrp", _O("qp"))

        repeat(5) {
            a.convertToString(null).recoverToSerializable<A>(null)
            Json.encodeToString(_a).let { Json.decodeFromString<_A>(it) }
        }

        measureTime {
            repeat(100) {
                a.convertToString(null).recoverToSerializable<A>(null)
            }
        }
        .let{ MLog(it) }

        measureTime {
            repeat(100) {
                Json.decodeFromString<_A>(Json.encodeToString(_a))
            }
        }
        .let{ MLog(it) }

        val data = Json.encodeToString(setOf(1 to "fp"))
        Json.decodeFromString<Pair<Int, String>>(data).let { MLog(it) }
    }

    class A(val i: Int, val d: Double, val s: String, val o: O): Serializable
    class O(val name: String): Serializable

    @kotlinx.serialization.Serializable
    class _A(val i: Int, val d: Double, val s: String, val o: _O)

    @kotlinx.serialization.Serializable
    class _O(val name: String)
}