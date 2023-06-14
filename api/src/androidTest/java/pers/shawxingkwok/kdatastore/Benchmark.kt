package pers.shawxingkwok.kdatastore

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.ktutil.roundToString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
internal class Benchmark {

    fun RandomString(size: Int): String =
        buildString {
            repeat(size) {
                append(('!'..'~').random())
            }
        }

    val size = 30
    val keys = (1..size).map { RandomString(7) }

    lateinit var info: Map<String, String>

    fun updateInfo() {
        info = keys.associateWith { RandomString(20) }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // warm up
    init {
        val sp = context.getSharedPreferences("_sp", Context.MODE_PRIVATE)
        val ds = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("_ds")
        }

        updateInfo()

        info.forEach { (key, value) ->
            sp.edit(true) {
                putString(key, value)
            }
            sp.getString(key, "")

            runBlocking {
                ds.edit {
                    it[stringPreferencesKey(key)] = value
                }
                ds.data.first()[stringPreferencesKey(key)]
            }
        }
        sp.edit(true){ clear() }
        runBlocking { ds.edit { it.clear() } }
        repeat(10) { getDuration(size) {  } }
    }

    @OptIn(ExperimentalTime::class, ExperimentalContracts::class)
    inline fun getDuration(divider: Int, act: () -> Unit): String {
        contract {
            callsInPlace(act, InvocationKind.EXACTLY_ONCE)
        }
        return (kotlin.time.measureTime(act).inWholeMicroseconds / (divider.toFloat())).roundToString(2)
    }

    val sp: SharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE)

    val ds: DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("ds")
        }

    init {
        MLog("sp read", getDuration(1) { sp.all.keys })

        getDuration(1) {
            runBlocking {
                ds.data.first()
            }
        }
        .let { MLog("ds read", it) }
    }

    @Test
    fun write() {
        // clear
        sp.edit(true){ clear() }

        runBlocking {
            ds.edit { it.clear() }
        }

        // place data in the first round
        updateInfo()

        info.forEach { (key, value) ->
            sp.edit(true) {
                putString(key, value)
            }
        }

        info.forEach { (keyName, value) ->
            runBlocking {
                ds.edit {
                    it[stringPreferencesKey(keyName)] = value
                }
            }
        }

        // log the actual runtime in the second round
        updateInfo()

        getDuration(size) {
            info.forEach { (key, value) ->
                sp.edit(true) {
                    putString(key, value)
                }
            }
        }
        .let { MLog("sp write", it) }

        getDuration(size) {
            info.forEach { (keyName, value) ->
                runBlocking {
                    ds.edit {
                        it[stringPreferencesKey(keyName)] = value
                    }
                }
            }
        }
        .let { MLog("ds write", it) }

        runBlocking {
            MLog(sp.all.size)
            MLog(ds.data.first().asMap().size)
        }
    }
}