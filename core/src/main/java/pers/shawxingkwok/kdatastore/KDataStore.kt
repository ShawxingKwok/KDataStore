package pers.shawxingkwok.kdatastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.updateIf
import java.io.*
import kotlin.reflect.full.functions

/**
 * A data store extension with little configuration, easy encryption and extensive supported types.
 *
 * See [usage](https://shawxingkwok.github.io/HomeSite/docs/kdatastore/).
 */
public abstract class KDataStore private constructor(
    private val fileName: String,
    @PublishedApi internal val defaultEncrypted: Boolean,
    @PublishedApi internal val encryption: Encryption?,
) {
    //region constructors
    public constructor(fileName: String = "settings")
        : this(fileName, false, null)

    public constructor(
        fileName: String = "settings",
        encryption: Encryption,
        defaultEncrypted: Boolean,
    )
        : this(fileName, defaultEncrypted, encryption)
    //endregion

    @PublishedApi
    internal var backupPrefs: MutablePreferences? = null

    @PublishedApi
    internal inline fun updateAll(prefs: MutablePreferences, act: () -> Unit): Preferences {
        backupPrefs = prefs
        act()
        return backupPrefs!!.also { backupPrefs = null }
    }

    //region migrations
    protected abstract interface Migration {
        public suspend fun shouldMigrate(): Boolean

        /**
         * Migrate from SharedPreferences, DataStore, or other sources, and
         * call [Flow.emit] with 'value' rather than 'transform'.
         *
         * Note that [onUpdate] in the delegate function wouldn't be called.
         * Warning: never call [EasyDSFlow.emit(transform: (T) -> T)].
         */
        public suspend fun migrate()

        public suspend fun cleanUp()
    }

    /**
     * See **TODO(website)**
     */
    protected open fun getMigration(context: Context): Migration =
        object : Migration {
            override suspend fun shouldMigrate(): Boolean = false
            override suspend fun migrate() {}
            override suspend fun cleanUp() {}
        }

    private val defaultDataMigration =
        // here's not safe to use 'getMigration' first.
        object : DataMigration<Preferences> {
            lateinit var migration: Migration

            override suspend fun shouldMigrate(currentData: Preferences): Boolean {
                migration = getMigration(MyInitializer.context)
                return migration.shouldMigrate()
            }

            override suspend fun migrate(currentData: Preferences): Preferences =
                updateAll(currentData.toMutablePreferences()) {
                    migration.migrate()
                }

            override suspend fun cleanUp() {
                migration.cleanUp()
            }
        }

    @PublishedApi
    internal val neededKeys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    private val keysFixDataMigration =
        object : DataMigration<Preferences> {
            lateinit var needlessKeys: Set<Preferences.Key<*>>

            override suspend fun shouldMigrate(currentData: Preferences): Boolean {
                needlessKeys = currentData.asMap().keys - neededKeys
                return needlessKeys.any()
            }

            override suspend fun migrate(currentData: Preferences): Preferences {
                val prefs = currentData.toMutablePreferences()
                needlessKeys.forEach { prefs -= it }
                return prefs
            }

            override suspend fun cleanUp() {}
        }
    //endregion

    @PublishedApi
    internal val handlerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    //region actualStore
    @PublishedApi
    internal val actualStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                val newPrefs = mutablePreferencesOf()
                //todo: remove 'runBlocking' after the official fix
                updateAll(newPrefs) {
                    runBlocking {
                        corruptionHandlers.forEach { it() }
                    }
                }
                newPrefs
            },
            migrations = listOf(defaultDataMigration, keysFixDataMigration),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName)
            }
        )
    //endregion

    @PublishedApi
    internal val caughtData: kotlinx.coroutines.flow.Flow<Preferences> =
        actualStore.data
        .catch {
            if (it is IOException) {
                it.printStackTrace()
                // maybe need revised
                val newPrefs = mutablePreferencesOf()
                updateAll(newPrefs) {
                    corruptionHandlers.forEach { it() }
                }
                emit(newPrefs)
            }
            else throw it
        }

    @PublishedApi
    internal val corruptionHandlers: MutableList<suspend () -> Unit> = mutableListOf()

    //region reset
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class Reset

    @Reset
    public suspend fun reset() {
        actualStore.edit { it.clear() }
    }
    //endregion

    public abstract class Flow<T : Any> @PublishedApi internal constructor(
        private val handlerScope: CoroutineScope,
        public val default: T
    ) : kotlinx.coroutines.flow.Flow<T> {
        public abstract suspend fun emit(value: T)

        /**
         * [transform]s the old value and emit it.
         */
        public abstract suspend fun emit(transform: (T) -> T)

        /**
         * Emits [value] in an async way.
         */
        public fun toss(value: T) {
            handlerScope.launch { emit(value) }
        }

        /**
         * [transform]s the old value and emits it in an async way.
         */
        public fun toss(transform: (T) -> T) {
            handlerScope.launch { emit(transform) }
        }
    }

    //region direct delegates
    private inline fun <reified T : Any> KDataStore.direct(
        default: T,
        encrypted: Boolean,
        noinline getKey: (String) -> Preferences.Key<T>,
        noinline onUpdate: suspend (T) -> Unit = {},
        noinline onCorrupt: suspend () -> T = { default },
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            getKey = if (encrypted) ::stringPreferencesKey else getKey,
            onEmit = onUpdate,
            onCorrupt = onCorrupt,
            convert =
            if (encrypted)
                { t: T -> t.toString().encrypt(encryption!!) }
            else
                null,
            recover =
            if (encrypted)
                { data: String -> data.decrypt(encryption!!).let(recover) }
            else
                null,
        )

    protected fun int(
        default: Int,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Int) -> Unit = {},
        onCorrupt: suspend () -> Int = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Int>> =
        direct(default, encrypted, ::intPreferencesKey, onUpdate, onCorrupt, String::toInt)

    protected fun long(
        default: Long,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Long) -> Unit = {},
        onCorrupt: suspend () -> Long = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Long>> =
        direct(default, encrypted, ::longPreferencesKey, onUpdate, onCorrupt, String::toLong)

    protected fun float(
        default: Float,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Float) -> Unit = {},
        onCorrupt: suspend () -> Float = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Float>> =
        direct(default, encrypted, ::floatPreferencesKey, onUpdate, onCorrupt, String::toFloat)

    protected fun double(
        default: Double,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Double) -> Unit = {},
        onCorrupt: suspend () -> Double = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Double>> =
        direct(default, encrypted, ::doublePreferencesKey, onUpdate, onCorrupt, String::toDouble)

    protected fun bool(
        default: Boolean,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Boolean) -> Unit = {},
        onCorrupt: suspend () -> Boolean = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Boolean>> =
        direct(default, encrypted, ::booleanPreferencesKey, onUpdate, onCorrupt, String::toBoolean)

    protected fun string(
        default: String,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (String) -> Unit = {},
        onCorrupt: suspend () -> String = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<String>> =
        direct(default, encrypted, ::stringPreferencesKey, onUpdate, onCorrupt) { it }
    //endregion

    //region converted delegates
    protected inline fun <reified T : Any> KDataStore.any(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onUpdate: suspend (T) -> Unit = {},
        noinline onCorrupt: suspend () -> T = { default },
        noinline convert: (T) -> String,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            getKey = ::stringPreferencesKey,
            onEmit = onUpdate,
            onCorrupt = onCorrupt,
            convert =
            if (encrypted)
                { t: T -> convert(t).encrypt(encryption!!) }
            else
                convert,
            recover =
                if (encrypted)
                    { data: String -> data.decrypt(encryption!!).let(recover) }
                else
                    recover,
        )

    protected fun byte(
        default: Byte,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Byte) -> Unit = {},
        onCorrupt: suspend () -> Byte = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Byte>> =
        any(default, encrypted, onUpdate, onCorrupt, convert = Any::toString, recover = String::toByte)

    protected fun short(
        default: Short,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Short) -> Unit = {},
        onCorrupt: suspend () -> Short = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Short>> =
        any(default, encrypted, onUpdate, onCorrupt, convert = Any::toString, recover = String::toShort)

    protected fun char(
        default: Char,
        encrypted: Boolean = defaultEncrypted,
        onUpdate: suspend (Char) -> Unit = {},
        onCorrupt: suspend () -> Char = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<Char>> =
        any(default, encrypted, onUpdate, onCorrupt, convert = Any::toString, recover = String::single)

    protected inline fun <reified T : Enum<T>> enum(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onUpdate: suspend (T) -> Unit = {},
        noinline onCorrupt: suspend () -> T = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> {
        val function = default::class.functions.first { it.name == "valueOf" }

        return any(
            default = default,
            encrypted = encrypted,
            convert = Any::toString,
            onUpdate = onUpdate,
            onCorrupt = onCorrupt,
            recover = {
                function.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> javaSerializable(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onUpdate: suspend (T) -> Unit = {},
        noinline onCorrupt: suspend () -> T = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            getKey = ::stringPreferencesKey,
            onEmit = onUpdate,
            onCorrupt = onCorrupt,
            convert = { t ->
                val bos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(bos)
                oos.writeObject(t)
                oos.close()
                bos.close()
                val bytes = bos.toByteArray().updateIf({ encrypted }, encryption!!::encrypt)
                Base64.encodeToString(bytes, Base64.DEFAULT)
            },
            recover = {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                    .updateIf({ encrypted }, encryption!!::decrypt)

                val bis = ByteArrayInputStream(bytes)
                val ois = ObjectInputStream(bis)
                ois.close()
                bis.close()
                ois.readObject() as T
            },
        )

    protected inline fun <reified T : Any> ktSerializable(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        noinline onUpdate: suspend (T) -> Unit = {},
        noinline onCorrupt: suspend () -> T = { default },
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        any(default, encrypted, onUpdate, onCorrupt, Json::encodeToString, Json::decodeFromString)
    //endregion
}