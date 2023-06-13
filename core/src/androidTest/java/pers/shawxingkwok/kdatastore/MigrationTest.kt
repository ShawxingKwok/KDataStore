package pers.shawxingkwok.kdatastore

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

internal object MigrationTest : KDataStore("previous"){
    val isVip by bool(false)
    val name by string("Jack")

    fun foo(){
        runBlocking {
            isVip.emit(true)
            name.value = "Shawxing"
            delay(1000)
        }
    }
}