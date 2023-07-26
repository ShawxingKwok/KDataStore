package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.*
import pers.shawxingkwok.kdatastore.hidden.MLog
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import java.io.IOException
import kotlin.reflect.KProperty

internal fun <T> KDSFlowDelegate(
    default: T,
    convert: (T & Any) -> String,
    recover: (String) -> T & Any,
)
: KReadOnlyProperty<KDataStore, KDSFlow<T>> =
    object : KReadOnlyProperty<KDataStore, KDSFlow<T>> {
        lateinit var flow: KDSFlow<T>

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            require(!thisRef.resetCalled) {
                "Use `reset` after all data properties are declared in ${thisRef.javaClass.canonicalName}."
            }

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

            fun save(prefs: MutablePreferences, value: T){
                if (value == default)
                    prefs -= key
                else
                    // default is value when nullable
                    prefs[key] = convert(value!!)
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
                    var corrupted = false

                    try {
                        thisRef.frontStore.edit { save(it, value) }
                    } catch (e: IOException) {
                        MLog.e(errMsg, e)
                        everCorrupted = true
                        corrupted = true
                    }

                    try {
                        thisRef.backupStore.edit { prefs ->
                            save(prefs, value)

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
                        MLog.e("${errMsg}bak.", e)
                    }
                }
                .launchIn(thisRef.handlerScope)
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDSFlow<T> = flow
    }