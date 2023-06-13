package pers.shawxingkwok.kdatastore

import org.junit.Test


class NullableInline {
    @Test
    fun foo(){
        bar<String?>()
        bar<String>()
    }
    var s: Int? = null
    inline fun <reified T> bar(){
        println(s is T)
        assert(null is T)
    }
}