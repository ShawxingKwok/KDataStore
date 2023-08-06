package pers.shawxingkwok.benchmark

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pers.shawxingkwok.androidutil.KLog
import pers.shawxingkwok.kdatastore.KDSFlow
import pers.shawxingkwok.kdatastore.KDataStore
import pers.shawxingkwok.ktutil.fastLazy
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class Benchmark(private val context: Context) {
    private val sp by fastLazy { context.getSharedPreferences("sp", Context.MODE_PRIVATE) }
    private val kv by fastLazy { MMKV.defaultMMKV() }
    private val Context.dataStore by preferencesDataStore("ds")
    private val ds by fastLazy { context.dataStore }
    private val kds by fastLazy { KDS }

    @OptIn(ExperimentalTime::class)
    private inline fun myMeasureTime(block: () -> Unit): String{
        val time = measureTime(block).inWholeMicroseconds / 1000.0
        return String.format("%.1f", time) + "ms"
    }

    private fun putInitialData(){
        KDS::class.declaredMemberProperties.take(30)
        .forEach { prop ->
            @Suppress("UNCHECKED_CAST")
            prop as KProperty1<KDS, KDSFlow<String>>
            val key = prop.name
            val value = "GJOPFGHNPIDFQKOP{FMPA</string>&#10;    "
            sp.edit(true) {
                putString(key, value)
            }

            kv.encode(key, value)

            runBlocking {
                ds.edit {
                    it[stringPreferencesKey(key)] = value
                }
            }

            prop.get(KDS).value = value
        }
    }

    private fun testSharedPreferences(){
        // start up
        // commit
        val initializationTime = myMeasureTime { sp.all }

        val commitTime = myMeasureTime {
            sp.edit(true){
                val value = sp.getString("s1", "")!!
                putString("s1", value.replaceFirstChar { it + 1 })
            }
        }

        KLog.w("SharedPreferences initialization: $initializationTime, commit: $commitTime.")
    }

    private fun testMmkv(){
        // start up
        KLog.w("MMKV initialization: ${myMeasureTime { kv.allKeys() }}.")
    }

    private fun testDataStore(){
        // respond
        CoroutineScope(Dispatchers.Default).launch {
            var time = 0L
            ds.data.onEach {
                it[stringPreferencesKey("s1")]
                if (time != 0L) {
                    val duration = String.format("%.1f", (System.nanoTime() - time) / 1000_000.0)
                    KLog.w("DataStore response: ${duration}ms.")
                }
            }
            .launchIn(this)

            delay(500)
            time = System.nanoTime()

            ds.edit { pref ->
                val key = stringPreferencesKey("s1")
                pref[key] = pref[key]!!.replaceFirstChar { it + 1 }
            }
        }
        runBlocking { delay(1000) }
    }

    private fun testKDataStore(){
        // start up
        KLog.w("KDataStore initialization: ${myMeasureTime { kds }}.")
    }

    @OptIn(KDataStore.CautiousApi::class)
    private fun clear(){
        sp.edit(true){ clear() }
        kv.edit(true) { clear() }
        runBlocking { ds.edit { it.clear() } }
        kds.delete()
    }

    init {
        if (!context.preferencesDataStoreFile("benchmark").exists()){
            clear()
            putInitialData()
            KLog.w("Put ${sp.all.size} sets of data fist. Invoke again to test the benchmark.")
        }else {
            testSharedPreferences()
            testMmkv()
            testDataStore()
            testKDataStore()
        }
        KLog.w("........................")
    }
}