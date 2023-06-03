package pers.shawxingkwok.preferencestore

import org.junit.Test

internal abstract class Super {
    val x = bar()
    abstract fun bar(): Int
}

internal class Sub : Super() {
    val y = -1

    @Test
    fun foo() {
        println(x)
    }

    override fun bar(): Int{
        println(y)
        return 2
    }
}