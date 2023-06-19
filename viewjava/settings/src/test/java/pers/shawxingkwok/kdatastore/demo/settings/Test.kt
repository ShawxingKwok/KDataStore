package pers.shawxingkwok.kdatastore.demo.settings

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Test {
    @OptIn(ExperimentalTime::class)
    @Test
    fun convertInt(){
        measureTime {
            repeat(10) {
                Json.encodeToString(it)
                    .let { Json.decodeFromString<Int>(it) }
            }
        }
        .let(::println)

        measureTime {
            repeat(10) {
                it.toString().toInt()
            }
        }
        .let(::println)

        measureTime {
            repeat(10){
                Json.encodeToString<A>(A(1))
                    .let { Json.decodeFromString<A>(it) }
                    .let(::println)
            }
        }
        .let(::println)
    }

    class A(val x: Int) : java.io.Serializable

    class E : java.io.Serializable
}