@file:Suppress("UNCHECKED_CAST")

package pers.apollokwok.prefstore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pers.apollokwok.ktutil.KReadOnlyProperty
import kotlin.reflect.KProperty

private fun <T: Any> PrefStore.FlowDelegate(
    default: T,
    encrypted: Boolean = defaultEncrypted,
    getKey: (String) -> Preferences.Key<*>,
    convert: ((T) -> String)?,
    recover: ((String) -> T)?,
    onEmit: suspend (T) -> Unit,
)
: KReadOnlyProperty<PrefStore, EasyDSFlow<T>> =
    object : KReadOnlyProperty<PrefStore, EasyDSFlow<T>> {
        private lateinit var flow: EasyDSFlow<T>

        override fun checkProperty(thisRef: PrefStore, property: KProperty<*>) {
            require(!encrypted || encryption != null) {
                "In ${thisRef.javaClass.canonicalName}, " +
                "'${property.name}' is encrypted with no encryption."
            }

            val key = getKey(property.name) as Preferences.Key<Any>

            val data: Flow<T> = caughtData
                .map { it[key] }
                .distinctUntilChanged()
                .map {
                    var value = it ?: return@map default

                    try {
                        if (encrypted)
                            value = encryption!!.decrypt(value as String)

                        if (recover != null)
                            value = recover(value as String)

                        value as T
                    } catch (e: Exception) {
                        val storePath = thisRef.javaClass.canonicalName
                        error("${e.message} The configuration of '$storePath' has been probably changed. " +
                              "If so in your test, delete the datastore file, " +
                              "or delete the application and reinstall first. " +
                              "If the previous configuration version is already released, " +
                              "set the migration in $storePath.")
                    }
                }

            flow = object : EasyDSFlow<T>(), Flow<T> by data {
                override val default: T = default

                override suspend fun emit(value: T) {
                    var converted: Any = value

                    if (convert != null)
                        converted = convert(converted as T)

                    if (encrypted)
                        converted = encryption!!.encrypt(converted as String)

                    onEmit(value)

                    prefStore.edit { it[key] = converted }
                }
            }
        }

        override fun getValue(thisRef: PrefStore, property: KProperty<*>): EasyDSFlow<T> = flow
    }

internal fun <T: Any> PrefStore.basic(
    default: T,
    encrypted: Boolean = defaultEncrypted,
    getKey: (String) -> Preferences.Key<T>,
    recover: (String) -> T,
    onEmit: (T) -> Unit = {},
)
: KReadOnlyProperty<PrefStore, EasyDSFlow<T>> =
    if (encrypted)
        FlowDelegate(default, true,::stringPreferencesKey, Any::toString, recover, onEmit)
    else
        FlowDelegate(default, false, getKey,null, null, onEmit)

public fun <T: Any> PrefStore.any(
    default: T,
    encrypted: Boolean = defaultEncrypted,
    onEmit: (T) -> Unit = {},
    convert: (T) -> String,
    recover: (String) -> T,
)
: KReadOnlyProperty<PrefStore, EasyDSFlow<T>> =
    FlowDelegate(default, encrypted, ::stringPreferencesKey, convert, recover, onEmit)