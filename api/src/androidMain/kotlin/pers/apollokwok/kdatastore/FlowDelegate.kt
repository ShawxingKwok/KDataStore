package pers.apollokwok.kdatastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.*
import pers.apollokwok.kdatastore.hidden.MLog
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import java.io.IOException
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T> FlowDelegate(
    default: T,
    noinline getKey: (String) -> Preferences.Key<*>,
    noinline convert: ((T & Any) -> String),
    noinline recover: ((String) -> T & Any),
)
: KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> =
    object : KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> {
        lateinit var flow: KDataStore.Flow<T>

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            @Suppress("UNCHECKED_CAST")
            val key = getKey(property.name) as Preferences.Key<Any>
            val src = thisRef.initialPrefs[key]
            val initialValue =
                if (src == null) default
                else recover(src as String)

            var onStart = true
            var everCorrupted = false

            flow = object : KDataStore.FlowImpl<T>(
                thisRef, MutableStateFlow(initialValue), key
            ){
                override fun reset() {
                    value = default
                }
            }

            val save: suspend (MutablePreferences, Any?) -> Unit =
                { prefs, convertedValue ->
                    if (convertedValue == null)
                        prefs -= key
                    else
                        prefs[key] = convertedValue
                }

            val errMsg = "Encounters IOException when writing data to dataStore ${thisRef.fileName}."

            flow
                // not writes to the disk when value is the initial
                .filterNot { value ->
                    if (onStart && value != initialValue)
                        onStart = false
                    onStart
                }
                .onEach { value ->
                    val converted: Any? =
                        // default is null when value is nullable
                        if (value == default) null
                        else convert(value!!)

                    var corrupted = false

                    try {
                        thisRef.frontStore.edit { save(it, converted) }
                    } catch (e: IOException) {
                        MLog.e(errMsg, e)
                        everCorrupted = true
                        corrupted = true
                    }

                    try {
                        thisRef.backupStore.edit { prefs ->
                            save(prefs, converted)

                            if (!everCorrupted) return@edit

                            val oldSet = prefs[thisRef.ioExceptionRecordsKey] ?: emptySet()

                            val newSet =
                                if (corrupted)
                                    oldSet + property.name
                                else
                                    oldSet - property.name

                            if (newSet.any())
                                prefs[thisRef.ioExceptionRecordsKey] = newSet
                            else
                                prefs -= thisRef.ioExceptionRecordsKey
                        }
                    } catch (e: IOException) {
                        MLog.e("${errMsg}bak.", tr = e)
                    }
                }
                .launchIn(thisRef.handlerScope)
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDataStore.Flow<T> = flow
    }