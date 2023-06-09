package pers.shawxingkwok.kdatastore

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

internal abstract class Super {
    val x = bar()
    abstract fun bar(): Int
}

internal class Sub : Super() {
    val y = -1

    @Test
    fun foo() {
        Json.encodeToString(arrayListOf(1)).let { Json.decodeFromString<ArrayList<Int>>(it) }.let(::println)
    }

    override fun bar(): Int{
        println(y)
        return 2
    }
}