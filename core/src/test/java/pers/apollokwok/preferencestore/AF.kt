package pers.apollokwok.preferencestore

import org.junit.Test

class AFG : EFA() {
    private var x = 1

    @Test
    fun foo() {
        bar(::x)
    }
}