package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.*
import pers.shawxingkwok.kdatastore.hidden.MLog
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import java.io.IOException
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T> FlowDelegate(
    default: T,
    noinline convert: (T & Any) -> String,
    noinline recover: (String) -> T & Any,
)
: KReadOnlyProperty<KDataStore, KDSFlow<T>> =
    object : KReadOnlyProperty<KDataStore, KDSFlow<T>> {
        lateinit var flow: KDSFlow<T>

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            val key = stringPreferencesKey(property.name)
            val src = thisRef.initialPrefs[key]
            val initialValue =
                if (src == null) default
                else recover(src)

            var onStart = true
            var everCorrupted = false

            val delegate = MutableStateFlow(initialValue)

            flow = object : KDSFlow<T>, MutableStateFlow<T> by delegate {
                init {
                    thisRef.kdsFlows += this
                    thisRef.keys += key
                }

                override fun reset() {
                    value = default
                }

                override val liveData: LiveData<T> by lazy(
                    mode = LazyThreadSafetyMode.PUBLICATION,
                    initializer = delegate::asLiveData
                )
            }

            val save: suspend (MutablePreferences, String?) -> Unit =
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
                    val converted: String? =
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

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDSFlow<T> = flow
    }