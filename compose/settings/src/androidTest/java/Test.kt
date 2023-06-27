import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.kdatastore.KDataStore
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
class Test {
    @OptIn(KDataStore.DangerousApi::class, ExperimentalTime::class)
    @Test
    fun start() {
        runBlocking {
            Settings.delete()
            Settings.isVip.value = true
            delay(1000)
            measureTime {
                Settings.delete()
            }
            .let { MLog(it) }
        }
    }
}