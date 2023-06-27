package pers.shawxingkwok.kdatastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteTest {
    @OptIn(KDataStore.DangerousApi::class)
    @Test
    fun start(){
        runBlocking {
            Settings.name.onEach { MLog(it) }.launchIn(this)
            delay(1000)
            Settings.name.value = "Jack"
            delay(200)
            Settings.name.reset()
            delay(200)
            Settings.name.value = "Shawxing"
            delay(5000)
            cancel()
        }
    }
}