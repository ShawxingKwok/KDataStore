package pers.apollokwok.prefstore

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainImmediateTest {
    @Test
    fun foo() = runBlocking{
        println(-3)
        launch(Dispatchers.Main.immediate) {
            println(1)
        }
        launch(Dispatchers.Main) {
            println(0)
        }
        println(2)
    }
}