package pers.apollokwok.prefstore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.apollokwok.ktutil.KReadOnlyProperty
import java.io.*
import kotlin.reflect.full.functions

public abstract class PrefStore private constructor(
    fileName: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    migrations: List<DataMigration<Preferences>>,
    scope: CoroutineScope,
    @PublishedApi internal val defaultEncrypted: Boolean,
    internal val encryption: Encryption?,
) {
    public constructor(
        fileName: String = "settings",
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        migrations: List<DataMigration<Preferences>> = emptyList(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )
        : this(fileName, corruptionHandler, migrations, scope, false, null)

    public constructor(
        fileName: String = "settings",
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        migrations: List<DataMigration<Preferences>> = emptyList(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        encryption: Encryption,
        defaultEncrypted: Boolean,
    )
        : this(fileName, corruptionHandler, migrations, scope, defaultEncrypted, encryption)

    internal val prefStore =
        PreferenceDataStoreFactory.create(
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope,
        ) {
            MyInitializer.context.preferencesDataStoreFile(fileName)
        }

    internal val caughtData =
        prefStore.data.catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else
                throw it
        }

    protected fun int(
        default: Int,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Int) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Int>> =
        basic(default, encrypted, ::intPreferencesKey, String::toInt, onEmit)

    protected fun long(
        default: Long,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Long) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Long>> =
        basic(default, encrypted, ::longPreferencesKey, String::toLong, onEmit)

    protected fun float(
        default: Float,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Float) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Float>> =
        basic(default, encrypted, ::floatPreferencesKey, String::toFloat, onEmit)

    protected fun double(
        default: Double,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Double) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Double>> =
        basic(default, encrypted, ::doublePreferencesKey, String::toDouble, onEmit)

    protected fun bool(
        default: Boolean,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Boolean) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Boolean>> =
        basic(default, encrypted, ::booleanPreferencesKey, String::toBoolean, onEmit)

    protected fun string(
        default: String,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (String) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<String>> =
        basic(default, encrypted, ::stringPreferencesKey, { it }, onEmit)

    protected fun byte(
        default: Byte,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Byte) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Byte>> =
        any(default, encrypted, onEmit, convert = Any::toString, recover = String::toByte)

    protected fun short(
        default: Short,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Short) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Short>> =
        any(default, encrypted, onEmit, convert = Any::toString, recover = String::toShort)

    protected fun char(
        default: Char,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (Char) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<Char>> =
        any(default, encrypted, onEmit, convert = Any::toString, recover = String::first)

    protected inline fun <reified T: Enum<T>> enum(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onEmit: (T) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<T>>
    {
        val function = default::class.functions.first { it.name == "valueOf" }
        return any(
            default = default,
            encrypted = encrypted,
            onEmit = onEmit,
            convert = Any::toString,
            recover = { function.call(it)!! as T }
        )
    }

    protected inline fun <reified T: Any> ktSerializable(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onEmit: (T) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<T>> =
        any(
            default = default,
            encrypted = encrypted,
            onEmit = onEmit,
            convert = Json::encodeToString,
            recover = Json::decodeFromString,
        )

    protected fun <T: Serializable> javaSerializable(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        onEmit: (T) -> Unit = {},
    )
    : KReadOnlyProperty<PrefStore, EasyDSFlow<T>> =
        any(
            default = default,
            encrypted = encrypted,
            onEmit = onEmit,
            convert = { t ->
                ByteArrayOutputStream()
                val bos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(bos)
                oos.writeObject(t)
                oos.close()
                bos.close()
                Json.encodeToString(bos.toByteArray())
            },
            recover = {
                val bytes = Json.decodeFromString<ByteArray>(it)
                val bis = ByteArrayInputStream(bytes)
                val ois = ObjectInputStream(bis)
                ois.close()
                bis.close()
                @Suppress("UNCHECKED_CAST")
                ois.readObject() as T
            },
        )

    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class Reset

    @Reset
    public suspend fun reset() {
        prefStore.edit { it.clear() }
    }
}