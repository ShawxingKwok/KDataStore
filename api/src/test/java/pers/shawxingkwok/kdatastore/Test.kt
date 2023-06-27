package pers.shawxingkwok.kdatastore

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.math.BigDecimal

class Test {
    @Test
    fun bar(){
        val content = Json.encodeToString(emptyList<Int>())
        Json.decodeFromString<List<Int>>(content).let(::println)
    }

    @Test
    fun foo(){
        runBlocking {
            launch {
                delay(1000)
                println(1)
            }

                runBlocking {
                    delay(2000)
                    println(2)
                }

            delay(3000)
        }
    }
}