package pers.shawxingkwok.kdatastore.demo.settings

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import pers.shawxingkwok.kdatastore.KDataStore

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("pers.shawxingkwok.kdatastore.demo.settings.test", appContext.packageName)
        runBlocking { delay(1000) }
        if (Settings.exist()) {
            // ...
            @OptIn(KDataStore.CautiousApi::class)
            Settings.delete()
        }
    }
}