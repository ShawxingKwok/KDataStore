package pers.shawxingkwok.kdatastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.ktutil.updateIf

@RunWith(AndroidJUnit4::class)
class NullabilityTest : KDataStore("nullabilityTest"){
    val isDark by javaSerializable(true)
    val language by enum<Language>(Language.GERMAN)
    val age by nullableJavaSerializable<Int>()

    @OptIn(DelicateApi::class)
    @Test
    fun start(){
        runBlocking {
            MLog(isDark.value)
            MLog(language.value)
            MLog(age.value)

            delay(1000)
        }
    }
}