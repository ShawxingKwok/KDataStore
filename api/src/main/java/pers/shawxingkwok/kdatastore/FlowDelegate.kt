package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.*
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import java.io.IOException
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T> FlowDelegate(
    default: T,
    noinline getKey: (String) -> Preferences.Key<*>,
    noinline convert: ((T & Any) -> String)?,
    noinline recover: ((String) -> T & Any)?,
)
: KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> =
    object : KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> {
        lateinit var flow: KDataStore.Flow<T>

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            @Suppress("UNCHECKED_CAST")
            val key = getKey(property.name) as Preferences.Key<Any>
            val src = thisRef.initialPrefs[key]
            val initialValue = when {
                src == null -> default
                recover != null -> recover(src as String)
                else -> (src as T)
            }

            var isOnResetAll = false
            var onStart = true
            var everCorrupted = false

            val keyTypeName =
                if (convert == null && (
                    T::class == Int::class || T::class == Long::class
                    || T::class == Float::class || T::class == Double::class
                    || T::class == String::class || T::class == Boolean::class
                    )
                )
                    T::class.simpleName
                else
                    "String"

            val ioExceptionInfo = "$keyTypeName ${property.name}"

            flow = object : KDataStore.FlowImpl<T>(
                thisRef, MutableStateFlow(initialValue), key
            ){
                override fun reset() {
                    value = default
                }

                override fun onResetAll() {
                    isOnResetAll = true
                    reset()
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
                // not writes to disk on resetAll
                .filterNot {
                    isOnResetAll.also {
                        if (it) isOnResetAll = false
                    }
                }
                .onEach { value ->
                    val converted: Any? =
                        when {
                            // default is null when value is nullable
                            value == default -> null
                            convert != null -> convert(value!!)
                            else -> value
                        }

                    var corrupted = false

                    try {
                        thisRef.frontStore.edit { save(it, converted) }
                    } catch (e: IOException) {
                        MLog.e(errMsg, tr = e)
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
                                    oldSet + ioExceptionInfo
                                else
                                    oldSet - ioExceptionInfo

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