package pers.shawxingkwok.kdatastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.updateIf
import java.io.IOException
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T : Any> FlowDelegate(
    default: T,
    noinline getKey: (String) -> Preferences.Key<*>,
    noinline convert: ((T) -> String)?,
    noinline recover: ((String) -> T)?,
)
: KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> =
    object : KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> {
        lateinit var flow: KDataStore.Flow<T>

        fun recoverFromSrc(src: Any?): T =
            when {
                src == null -> default
                recover != null -> recover(src as String)
                else -> (src as T)
            }

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            @Suppress("UNCHECKED_CAST")
            val key = getKey(property.name) as Preferences.Key<Any>
            thisRef.keys += key

            val data: kotlinx.coroutines.flow.Flow<T> = thisRef.caughtData
                .map { it[key] }
                .distinctUntilChanged()
                .map(::recoverFromSrc)

            flow = object : KDataStore.Flow<T>(thisRef.handlerScope, default) {
                override suspend fun collect(collector: FlowCollector<T>) {
                    data.collect(collector)
                }

                override suspend fun emit(value: T) {
                    val converted = (value as Any).updateIf({ convert != null }){ convert!!(value) }

                    when (val prefs = thisRef.migratedPrefs ?: thisRef.updatedAllPrefs) {
                        // When IOException occurs, the flow collector wouldn't be activated, which encourages
                        // user to edit again.
                        null -> {
                            try {
                                thisRef.backup.edit { it[key] = converted }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            try {
                                thisRef.actualStore.edit { it[key] = converted }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                        else -> prefs[key] = converted
                    }
                }

                override suspend fun emit(transform: (T) -> T) {
                    val value = when (val prefs = thisRef.migratedPrefs ?: thisRef.updatedAllPrefs) {
                        null -> transform(first())
                        else -> {
                            val src = prefs[key]
                            val recovered = recoverFromSrc(src)
                            transform(recovered)
                        }
                    }
                    emit(value)
                }
            }
        }

         override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDataStore.Flow<T> = flow
    }