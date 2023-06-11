package pers.shawxingkwok.kdatastore

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class MigrationTest : KDataStore(){
    val isVip by bool(false)
    val name by string("Jack")

    @Test
    fun foo(){
        runBlocking {
            isVip.emit(true)
            name.value = "Shawxing"
            assert(isVip.first())
            assert(name.first() == "Shawxing")
        }
    }
}