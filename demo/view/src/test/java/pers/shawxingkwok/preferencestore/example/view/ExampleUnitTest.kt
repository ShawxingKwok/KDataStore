package pers.shawxingkwok.preferencestore.example.view

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val info =  "{\"s\":\"s\",\"i\":2}"
        Json.decodeFromString<LAFA>(info).let(::println)
    }
}

@Serializable
data class LAFA(val a: String, val i: Int = 1)