package pers.shawxingkwok.kdatastore

import org.junit.Test
import java.io.Serializable
import java.util.concurrent.CyclicBarrier
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Runtime {
    @kotlinx.serialization.Serializable
    class A(val x: Int = 0, val b: String = "gnofgbnr", val j: Long = 57389011201)

    class B(val b: String = "gnofgbnr", val xjfgp: Int = 0, val j: Double = 57389011201.0): Serializable

    @OptIn(ExperimentalTime::class)
    @Test
    fun start(){
        var x: Any = 0

        var cypher: Cypher? = Cypher.AES(
            "Fp",
            "Gf",
            ivBytes = (1..16).map { it.toByte() }.toByteArray(),
        )

        repeat(5){
            measureTime {
                x = B().convertToString(cypher).recoverToSerializable<B>(cypher)
            }
            .let(::println)
        }

        measureTime {
            repeat(10){
                x = B().convertToString(cypher).recoverToSerializable<B>(cypher)
            }
        }
        .let(::println)

        println("---------------")

        cypher = null
        measureTime {
            repeat(10){
                x = B().convertToString(cypher).recoverToSerializable<B>(cypher)
            }
        }
        .let(::println)

        println(x)
    }
}