package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.*
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.updateIf
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T : Any> FlowDelegate(
    default: T,
    noinline getKey: (String) -> Preferences.Key<*>,
    noinline convert: ((T) -> String)?,
    noinline recover: ((String) -> T)?,
)
: KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
    object : KReadOnlyProperty<KDataStore, MutableStateFlow<T>> {
        lateinit var flow: MutableStateFlow<T>

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

            flow = MutableStateFlow(initialValue)
            var onStart = true

            // not writes the initial value to the disk
            flow.onEach { value ->
                if (value != initialValue)
                    onStart = false

                if (onStart) return@onEach

                val converted = (value as Any).updateIf({ convert != null }){ convert!!(value) }

                try {
                    thisRef.frontStore.edit {
                        it[key] = converted
                    }
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

                        thisRef.frontStore.edit {
                            val newSet = (it[thisRef.ioExceptionRecordsKey] ?: emptySet()) + ioExceptionInfo
                            it[thisRef.ioExceptionRecordsKey] = newSet
                        }
                    }catch (e: IOException){
                        MLog.e(e)
                    }
                }

                try {
                    thisRef.backupStore.edit {
                        it[key] = converted
                    }
                }catch (e: IOException){
                    MLog.e(e)
                }
            }
            .launchIn(thisRef.handlerScope)
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): MutableStateFlow<T> = flow
    }