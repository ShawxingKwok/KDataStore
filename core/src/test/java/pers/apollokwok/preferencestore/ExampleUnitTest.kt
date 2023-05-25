package pers.apollokwok.preferencestore

import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.charset.Charset
import kotlin.reflect.KMutableProperty

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun foo(){
        val src = MutableStateFlow(1)
        val flow = src.map { it * 10 }.onEach { src.emit(2) }

        runBlocking {
            flow.collect{
                println(it)
            }
        }
    }
}