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
class DeleteResetTest {
    @OptIn(KDataStore.DangerousApi::class)
    @Test
    fun start(){
        runBlocking {
            Settings.delete()
            MLog(Settings.exists())

            Settings.reset()

            Settings.name.onEach { MLog(it) }.launchIn(this)
            delay(200)
            Settings.name.value = "Jack"

            delay(200)
            Settings.name.reset()

            delay(200)
            Settings.name.value = "Shawxing"

            delay(200)
            Settings.reset()

            delay(1000)
            cancel()
        }
    }
}