package pers.shawxingkwok.kdatastore

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
}