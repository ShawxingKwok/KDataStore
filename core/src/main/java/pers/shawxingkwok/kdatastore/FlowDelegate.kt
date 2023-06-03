package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import java.io.IOException
import kotlin.reflect.KProperty

// Use inline to help check cast
@PublishedApi
internal inline fun <reified T : Any> FlowDelegate(
    default: T,
    encrypted: Boolean,
    noinline getKey: (String) -> Preferences.Key<*>,
    noinline onEmit: suspend (T) -> Unit,
    noinline onCorrupt: suspend () -> T,
    noinline convert: ((T) -> String)?,
    noinline recover: ((String) -> T)?,
)
: KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> =
    object : KReadOnlyProperty<KDataStore, KDataStore.Flow<T>> {
        lateinit var flow: KDataStore.Flow<T>

        suspend fun recoverFromSrc(path: String, src: Any?): T =
            try {
                when {
                    src == null -> default
                    recover != null -> recover(src as String)
                    else -> (src as T)
                }
            } catch (e: Exception) {
                val backupValue = onCorrupt()
                flow.emit(backupValue)
                MLog.e("$path corrupted and the value $backupValue got via 'onCorrupt' is invoked.")
                backupValue
            }

        override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
            thisRef.corruptionHandlers += {
                flow.emit(onCorrupt())
            }

            val locatedProp = "${thisRef.javaClass.canonicalName}.${property.name}"
            require(!encrypted || thisRef.encryption != null) {
                "$locatedProp is encrypted without encryption."
            }

            @Suppress("UNCHECKED_CAST")
            val key = getKey(property.name) as Preferences.Key<Any>
            thisRef.neededKeys += key

            val data: kotlinx.coroutines.flow.Flow<T> = thisRef.caughtData
                .map { it[key] }
                .distinctUntilChanged()
                .map { recoverFromSrc(locatedProp, it) }

            flow = object : KDataStore.Flow<T>(thisRef.handlerScope, default) {
                override suspend fun collect(collector: FlowCollector<T>) {
                    require(thisRef.backupPrefs == null) {
                        "Collection is forbidden in migration and 'onCorrupt."
                    }
                    data.collect(collector)
                }

                override suspend fun emit(value: T) {
                    onEmit(value)

                    var converted: Any = value

                    if (convert != null)
                        converted = convert(converted as T)

                    when (val prefs = thisRef.backupPrefs) {
                        // When IOException occurs, the flow collector wouldn't be activated, which encourages
                        // user to edit again.
                        null ->
                            try {
                                thisRef.actualStore.edit { it[key] = converted }
                            }catch (e: IOException){
                                e.printStackTrace()
                            }

                        else -> prefs[key] = converted
                    }
                }

                override suspend fun emit(transform: (T) -> T) {
                    val value = when (val prefs = thisRef.backupPrefs) {
                        null -> transform(first())
                        else -> {
                            val src = prefs[key]
                            val recovered = recoverFromSrc(locatedProp, src)
                            transform(recovered)
                        }
                    }
                    emit(value)
                }
            }
        }

        override fun getValue(thisRef: KDataStore, property: KProperty<*>): KDataStore.Flow<T> = flow
    }