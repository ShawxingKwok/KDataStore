package pers.shawxingkwok.kdatastore

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Timer
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(AndroidJUnit4::class)
class CorruptionTest  {
    val ds = PreferenceDataStoreFactory.create {
        MyInitializer.context.preferencesDataStoreFile("corruptionTest")
    }

    @Test
    fun start(){
        runBlocking {
            MLog("(CorruptionTest.kt:26)")
            ds.data.first()
            ds.edit {
                it[stringPreferencesKey("Fd")] = "gfrigk"
            }
        }
    }
}