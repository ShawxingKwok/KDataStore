package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

            val qualifiedPropName = "${thisRef.javaClass.canonicalName}.${property.name}"

            val key = qualifiedPropName
                .encryptIfNeeded(thisRef.cipher)
                .let(::stringPreferencesKey)

            thisRef.keys += key

            val initialValue =
                when(val src = thisRef.initialPrefs[key]){
                    null -> default
                    else -> recover(src)
                }

            var onStart = true
            var everCorrupted = false

            val delegate = MutableStateFlow(initialValue)

            flow = object : KDSFlow<T>, MutableStateFlow<T> by delegate {
                init {
                    thisRef.kdsFlows += this
                }

                override fun reset() {
                    value = default
                }

                override val liveData: LiveData<T> by lazy(
                    mode = LazyThreadSafetyMode.PUBLICATION,
                    initializer = delegate::asLiveData
                )
            }

            fun save(prefs: MutablePreferences, converted: String?){
                if (converted == null)
                    prefs -= key
                else
                    prefs[key] = converted
            }

            thisRef.handlerScope.launch {
                thisRef.initialCorrectingJob?.join()

                flow
                    // not writes to the disk when value is the initial
                    .filterNot { value ->
                        if (onStart && value != initialValue)
                            onStart = false
                        onStart
                    }
                    .collect { value ->
                        var corrupted = false

                        // default is null when nullable
                        val converted = if (value == default) null else convert(value!!)

                        try {
                            thisRef.frontStore.edit { save(it, converted) }
                        } catch (e: IOException) {
                            MLog.d("Encountered io exception when storing ${property.name} with $value.")
                            everCorrupted = true
                            corrupted = true

                            thisRef.ioScope.launch {
                                thisRef.getIOExceptionTagFile().createNewFile()
                            }
                        }

                        try {
                            thisRef.backupStore.edit { prefs ->
                                save(prefs, converted)

                                if (!everCorrupted) return@edit

                                val oldSet = prefs[thisRef.ioExceptionRecordsKey] ?: emptySet()

                                val newSet =
                                    if (corrupted)
                                        oldSet + key.name
                                    else
                                        oldSet - key.name

                                if (newSet.any())
                                    prefs[thisRef.ioExceptionRecordsKey] = newSet
                                else
                                    prefs -= thisRef.ioExceptionRecordsKey
                            }
                        } catch (e: IOException) {
                            if (corrupted)
                                //todo: consider throwing out
                                MLog.e(
                                    obj = "Encountered IOException when writing $qualifiedPropName to datastore and its backup.",
                                    tr = e,
                                )

                            thisRef.ioScope.launch {
                                thisRef.getIOExceptionTagFile().createNewFile()
                            }
                        }
                    }
            }
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDSFlow<T> = flow
    }