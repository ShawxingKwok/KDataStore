package pers.shawxingkwok.kdatastore

import android.content.Context
import android.util.Log
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
internal class GeneralDataStore {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    val Context.dataStore by preferencesDataStore("test")
    val dataStore = context.dataStore

    val key = stringPreferencesKey("a")

    val initialPreferences: Preferences
    init {
        measureTime {
            runBlocking {
                initialPreferences = dataStore.data.first()
            }
        }
        .let { MLog(it) }
    }

    val flow = dataStore.data.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Lazily,
        initialValue = initialPreferences,
    )

    @Test
    fun useAppContext() {
        runBlocking {
            MLog(flow.value[key])

            dataStore.data.collect{
                MLog(it[key])
            }
        }
    }
}