package pers.shawxingkwok.kdatastore

import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
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
        val flowDelegate = MutableStateFlow(1)

        val flow = object : MutableStateFlow<Int> by flowDelegate{
            override var value: Int = flowDelegate.value
                get() = flowDelegate.value
                set(value) {
                    if (field == value) return

                    field = value
                    flowDelegate.value = value
                    println("emitted $value")
                }

        }

        runBlocking {
            println("emitting")
            flow.emit(2)
            flow.value++
        }
    }
}