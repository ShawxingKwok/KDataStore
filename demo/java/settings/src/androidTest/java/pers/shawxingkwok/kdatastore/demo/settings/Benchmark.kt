package pers.shawxingkwok.kdatastore.demo.settings

import android.content.Context
import android.content.SharedPreferences
import android.text.method.TextKeyListener.clear
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.StringFormat
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.androidutil.AppContext
import pers.shawxingkwok.androidutil.KLog
import pers.shawxingkwok.kdatastore.KDSFlow
import pers.shawxingkwok.kdatastore.KDataStore
import pers.shawxingkwok.kdatastore.demo.settings.BenchMarkKDS.s1
import pers.shawxingkwok.ktutil.lazyFast
import java.io.File
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
internal class Benchmark {
    val sp by lazyFast { AppContext.getSharedPreferences("sp", Context.MODE_PRIVATE) }
    val kv by lazyFast {
        MMKV.initialize(AppContext)
        MMKV.defaultMMKV()
    }
    val Context.dataStore by preferencesDataStore("ds")
    val ds by lazyFast { AppContext.dataStore }
    val kds by lazyFast { BenchMarkKDS }

    @OptIn(KDataStore.CautiousApi::class)
    fun clearAll(){
        sp.edit(true){ clear() }

        kv.edit(true) { clear() }

        runBlocking {
            ds.edit { it.clear() }
        }

        kds.delete()
    }

    fun putInitialData(){
        BenchMarkKDS::class.declaredMemberProperties
        .forEach { prop ->
            prop as KProperty1<BenchMarkKDS, KDSFlow<String>>
            val key = prop.name
            val value = "GJOPFGHNPIDFQKOP{FMPA"

            sp.edit(true) {
                putString(key, value)
            }

            kv.encode(key, value)

            runBlocking {
                ds.edit {
                    it[stringPreferencesKey(key)] = value
                }
            }

            prop.get(BenchMarkKDS).value = value
        }

        runBlocking { delay(1000) }
    }

    fun testSharedPreferences(){
        // start up
        // commit
        val initializationTime = measureTime { sp.all }

        val commitTime = measureTime {
            sp.edit(true){
                val value = sp.getString("s1", "")!!
                putString("s1", value.replaceFirstChar { it + 1 })
            }
        }

        KLog.d("SharedPreferences initialization: $initializationTime, commit: $commitTime.")
    }

    fun testMmkv(){
        // start up
        KLog.d("MMKV initialization: ${measureTime { kv.allKeys() }}.")
    }

    fun testDataStore(){
        // respond
        CoroutineScope(Dispatchers.Main.immediate).launch {
            var time = 0L
            ds.data.onEach {
                it[stringPreferencesKey("s1")]
                if (time != 0L)
                    KLog.d("DataStore response: ${System.currentTimeMillis() - time}ms.")
            }
            .launchIn(this)

            delay(1000)
            time = System.currentTimeMillis()

            ds.edit { pref ->
                val key = stringPreferencesKey("s1")
                pref[key] = pref[key]!!.replaceFirstChar { it + 1 }
            }
        }

        runBlocking { delay(2000) }
    }

    fun testKDataStore(){
        // start up
        KLog.d("KDataStore initialization: ${measureTime { kds }}.")
    }

    @Test
    fun start() {
        if (!AppContext.preferencesDataStoreFile("benchmark").exists()){
            putInitialData()
            KLog.d("Put ${sp.all.size} sets of data fist. Invoke again to test the benchmark.")
            KLog.d("........................")
            return
        }

        testSharedPreferences()
        testMmkv()
        testDataStore()
        testKDataStore()
        KLog.d("...............")
    }
}