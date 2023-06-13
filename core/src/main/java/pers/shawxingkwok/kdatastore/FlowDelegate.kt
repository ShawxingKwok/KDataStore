package pers.shawxingkwok.kdatastore

import android.icu.text.ListFormatter
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.*
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.allDo
import pers.shawxingkwok.ktutil.updateIf
import java.io.IOException
import kotlin.reflect.KClass
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
            thisRef.keys += key
            val src = thisRef.initialPrefs[key]

            val initialValue = when {
                src == null -> default
                recover != null -> recover(src as String)
                else -> (src as T)
            }

            flow = object : KDataStore.Flow<T>, MutableStateFlow<T> by MutableStateFlow(initialValue){
                override fun reset() {
                    value = default
                }
            }

            thisRef.flows += flow

            var onStart = true

            val save: suspend (DataStore<Preferences>, Any?) -> Unit =
                { store, convertedValue ->
                    store.edit {
                        if (convertedValue == null)
                            it -= key
                        else
                            it[key] = convertedValue
                    }
                }

            // not writes the initial value to the disk
            flow.onEach { value ->
                if (value != initialValue)
                    onStart = false

                if (onStart) return@onEach

                val converted: Any? =
                    when{
                        value == null -> null
                        convert != null -> convert(value)
                        else -> value
                    }

                try {
                    save(thisRef.frontStore, converted)
                } catch (e: IOException) {
                    try {
                        val typeName =
                            if (convert == null &&
                                T::class in arrayOf<KClass<*>>(
                                    Int::class, Long::class,
                                    Float::class, Double::class,
                                    String::class, Boolean::class
                                )
                            )
                                T::class.simpleName
                            else
                                "String"

                        val ioExceptionInfo = "$typeName ${property.name}"

                        thisRef.backupStore.edit {
                            val oldSet = it[thisRef.ioExceptionRecordsKey] ?: emptySet()
                            it[thisRef.ioExceptionRecordsKey] = oldSet + ioExceptionInfo
                        }
                    }catch (e: IOException){
                        MLog.e(e)
                    }
                }

                try {
                    save(thisRef.backupStore, converted)
                }catch (e: IOException){
                    MLog.e(e)
                }
            }
            .launchIn(thisRef.handlerScope)
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDataStore.Flow<T> = flow
    }