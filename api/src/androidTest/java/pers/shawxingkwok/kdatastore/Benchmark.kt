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
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
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

    val size = 300
    val keys = (1..size).map { RandomString(7) }

    lateinit var info: Map<String, String>

    fun updateInfo() {
        info = keys.associateWith { RandomString(20) }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @OptIn(ExperimentalTime::class)
    inline fun logDuration(name: String, times: Int, act: () -> Unit) {
        val duration =
            measureTime {
                repeat(times) { act() }
            }

        MLog(name, duration.inWholeMicroseconds / times)
    }

    inline fun logDurationWithInfo(header: String, act: (String, String) -> Unit) {
        val duration = measureTime {
            info.forEach { (key, value) ->
                act(key, value)
            }
        }

        MLog(header, duration.inWholeMicroseconds / info.size)
    }

    lateinit var sp: SharedPreferences
    lateinit var ds: DataStore<Preferences>
    lateinit var kv: MMKV

    fun launchWithRead(){
        MLog("launch with reading")

        logDuration("sp", 1) {
            sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE)
            sp.all
        }

        logDuration("kv", 1){
            MMKV.initialize(context)
            kv = MMKV.defaultMMKV()
            kv.allKeys()
        }

        logDuration("ds", 1) {
            ds = PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile("ds")
            }

            runBlocking {
                ds.data.first()
            }
        }
    }

    fun read() {
        MLog("read")

        logDuration("sp", 10) { sp.all }

        logDuration("kv",10) { kv.allKeys() }

        logDuration("ds", 10) {
            runBlocking {
                ds.data.first()
            }
        }
    }

    fun write() {
        MLog("write")

        // clear
        sp.edit(true){ clear() }

        kv.edit(true) { clear() }

        runBlocking {
            ds.edit { it.clear() }
        }

        // log the actual runtime in the second round
        updateInfo()

        logDurationWithInfo("sp") { key, value ->
            sp.edit(true) {
                putString(key, value)
            }
        }

        logDurationWithInfo("kv") { key, value ->
            kv.encode(key, value)
        }

        logDurationWithInfo("ds") { keyName, value ->
            runBlocking {
                ds.edit {
                    it[stringPreferencesKey(keyName)] = value
                }
            }
        }
    }

    @Test
    fun start() {
        launchWithRead()
        read()
        write()
        val key = "GSIGN"
        val value = "Gpjqtgj"

        sp.edit {
            putString(key, value)
        }

        logDuration("sp", 1){
            sp.getString(key, "")
        }

        kv.encode(key, value)
        logDuration("kv", 1) {
            kv.decodeString(key)
        }

        runBlocking {
            ds.edit {
                it[stringPreferencesKey(key)] = value
            }
        }

        runBlocking {
            logDuration("ds",1){
                ds.data.first()
            }
        }
        MLog("...............")
    }
}