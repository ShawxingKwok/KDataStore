package pers.shawxingkwok.kdatastore

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.ktutil.roundToString
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
internal class Benchmark {
    val enlargedTimes = 1

    fun RandomString(length: Int): String =
        buildString {
            repeat(length * enlargedTimes) {
                append(('!'..'~').random())
            }
        }

    val keys = (1..30).map { RandomString(7) }

    lateinit var info: Map<String, String>

    fun updateInfo() {
        info = keys.associateWith { RandomString(20) }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @OptIn(ExperimentalTime::class)
    inline fun logDuration(header: String, times: Int, act: () -> Unit) {
        val duration =
            measureTime {
                repeat(times) { act() }
            }

        MLog("$header: ${duration.inWholeMicroseconds / times} µs")
    }

    @OptIn(ExperimentalTime::class)
    inline fun logDurationWithInfo(header: String, act: (String, String) -> Unit) {
        val duration = measureTime {
            info.forEach { (key, value) ->
                act(key, value)
            }
        }

        MLog("$header: ${duration.inWholeMicroseconds / info.size} µs")
    }

    lateinit var sp: SharedPreferences
    lateinit var kv: MMKV
    val Context.dataStore by preferencesDataStore("ds")
    lateinit var ds: DataStore<Preferences>
    lateinit var kds: Settings

    fun launch(){
        logDuration("sp", 1) {
            sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE)
        }

        logDuration("kv", 1){
            MMKV.initialize(context)
            kv = MMKV.defaultMMKV()
        }

        logDuration("ds with reading", 1) {
            ds = context.dataStore

            runBlocking {
                ds.data.first()
            }
        }

        logDuration("kds", 1){
            kds = Settings()
        }

        val spLength = (sp.all.values.firstOrNull() as String?)?.length ?: 0
        val kdsLength = kds.fjiodfg.value.length
        require(spLength == kdsLength){
            clearAll()
            "$spLength, $kdsLength"
        }
        MLog("launch with size ${sp.all.size} * $spLength")
    }

    fun clearAll(){
        sp.edit(true){ clear() }

        kv.edit(true) { clear() }

        runBlocking {
            ds.edit { it.clear() }
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

        logDuration("kds", 10){
            kds.fgegjiod.value
        }
    }

    fun write() {
        clearAll()

        // log the actual runtime in the second round
        updateInfo()
        MLog("write with size ${info.size}")

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

        var i = 0
        val props = Settings::class.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }
        logDurationWithInfo("kds"){ _, value ->
            val flow = props[i++].get(kds) as KDataStore.Flow<String>
            flow.value = value
        }
    }

    fun respond(){
        updateInfo()
        dsRespond()
        kdsRespond()
    }

    fun kdsRespond(){
        val writeTime = mutableListOf<Long>()
        val readTime = mutableListOf<Long>()

        runBlocking {
            var i = 0

            launch {
                kds.fgegjiod.collect {
                    if (i != 0) readTime += System.nanoTime()
                    if (i == info.size) cancel()
                }
            }

            info.values.forEach {
                writeTime += System.nanoTime()
                kds.fgegjiod.value = it
                i++
                delay(200)
            }
        }
        val diff = readTime.takeLast(info.size).average() - writeTime.average()
        MLog("kds respond", (diff / 1000_000.0).roundToString(1) + " ms")
    }

    fun dsRespond(){
        val writeTime = mutableListOf<Long>()
        val readTime = mutableListOf<Long>()

        var i = 0

        runBlocking {
            launch {
                ds.data.collect {
                    if(i != 0) readTime += System.nanoTime()
                    if (i == info.size) cancel()
                }
            }

            info.forEach { (keyName, value) ->
                writeTime += System.nanoTime()
                ds.edit {
                    it[stringPreferencesKey(keyName)] = value
                    i++
                }
            }
        }
        val diff = readTime.takeLast(info.size).average() - writeTime.average()
        MLog("ds respond", (diff / 1000_000.0).roundToString(1) + " ms")
    }

    @Test
    fun start() {
        // return
        launch()
        read()
        write()
        respond()
        MLog("...............")
    }
}