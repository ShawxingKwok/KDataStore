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
 * An extended data store with little configuration, easy encryption and extensive supported types.
 *
 * See [tutorial](https://shawxingkwok.github.io/ITWorks/docs/kdatastore/).
 */
public abstract class KDataStore private constructor(
    private val fileName: String,
    @PublishedApi internal val handlerScope: CoroutineScope,
    ioScope: CoroutineScope,
    @PublishedApi internal val defaultEncrypted: Boolean,
    @PublishedApi internal val encryption: Encryption?,
) {
    //region constructors
    public constructor(
        fileName: String = "settings",
        handlerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )
        : this(fileName, handlerScope, ioScope, false, null)

    public constructor(
        fileName: String = "settings",
        handlerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        encryption: Encryption,
        defaultEncrypted: Boolean,
    )
        : this(fileName, handlerScope, ioScope, defaultEncrypted, encryption)
    //endregion

    public abstract class Flow<T : Any> @PublishedApi internal constructor(
        private val handlerScope: CoroutineScope,
        public val default: T
    )
        : kotlinx.coroutines.flow.Flow<T>
    {
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

    //region fun updateAll
    @PublishedApi
    internal var updatedAllPrefs: MutablePreferences? = null

    /**
     * Only writes once to the disk.
     *
     * Note: this is needless in [getMigration].
     */
    public suspend fun updateAll(act: suspend () -> Unit) {
        updatedAllPrefs = actualStore.data.first().toMutablePreferences()
        act()
        actualStore.updateData { updatedAllPrefs!! }
        updateBackupStore(updatedAllPrefs!!)
        updatedAllPrefs = null
    }
    //endregion

    @PublishedApi
    internal suspend fun updateBackupStore(prefs: Preferences){
        backupStore.edit {
            fixer.backupKeys.forEach { key ->
                @Suppress("UNCHECKED_CAST")
                key as Preferences.Key<Any>
                it[key] = prefs[key] ?: return@forEach
            }
        }
    }

    //region migrations
    @PublishedApi
    internal var migratedPrefs: MutablePreferences? = null

    protected interface Migration {
        public suspend fun shouldMigrate(): Boolean

        /**
         * Migrate from SharedPreferences, DataStore, or other sources, and
         * call [Flow.emit] with 'value' rather than 'transform'.
         */
        public suspend fun migrate()

        public suspend fun cleanUp()
    }

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

            override suspend fun migrate(currentData: Preferences): Preferences {
                migratedPrefs = currentData.toMutablePreferences()
                migration.migrate()
                updateBackupStore(migratedPrefs!!)
                return migratedPrefs!!
            }

            override suspend fun cleanUp() {
                migration.cleanUp()
                migratedPrefs = null
            }
        }
    //endregion

    @PublishedApi
    internal val fixer: Fixer = Fixer()

    //region actualStore backupStore
    @PublishedApi
    internal val actualStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                //todo: remove 'runBlocking' after the official fix
                runBlocking {
                    backupStore.data.first()
                }
            },
            migrations = listOf(defaultDataMigration, fixer.generalFixDataMigration),
            scope = ioScope,
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName)
            }
        )

    @PublishedApi
    internal val backupStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(fixer.backupFixDataMigration),
            scope = ioScope,
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName + "_backup")
            }
        )
    //endregion

    @PublishedApi
    internal val caughtData: kotlinx.coroutines.flow.Flow<Preferences> =
        actualStore.data
        .catch { tr ->
            if (tr !is IOException) throw tr

            tr.printStackTrace()

            val backupPrefs = backupStore.data
                .catch {
                    if (it !is IOException) throw it
                    it.printStackTrace()
                    emit(emptyPreferences())
                }
                .first()

            emit(backupPrefs)
        }

    //region delicate functions: reset delete exists
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class Delicate

    @Delicate
    public suspend fun reset() {
        actualStore.updateData { emptyPreferences() }
        backupStore.updateData { emptyPreferences() }
    }

    private fun getFile() = File(MyInitializer.context.filesDir, "datastore/$fileName.preferences_pb")
    private fun getBackupFile() = File(MyInitializer.context.filesDir, "datastore/${fileName}_backup.preferences_pb")

    @Delicate
    public fun delete(): Boolean {
        return getFile().delete() && getBackupFile().delete()
    }

    public fun exists(): Boolean{
        return getFile().exists()
    }
    //endregion

    //region direct delegates
    private inline fun <reified T : Any> KDataStore.direct(
        default: T,
        encrypted: Boolean,
        backup: Boolean,
        noinline getKey: (String) -> Preferences.Key<T>,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            backup = backup,
            actualStore = actualStore,
            backupStore = backupStore,
            getKey = if (encrypted) ::stringPreferencesKey else getKey,
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
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Int>> =
        direct(default, encrypted, backup, ::intPreferencesKey, String::toInt)

    protected fun long(
        default: Long,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Long>> =
        direct(default, encrypted, backup, ::longPreferencesKey, String::toLong)

    protected fun float(
        default: Float,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Float>> =
        direct(default, encrypted, backup, ::floatPreferencesKey, String::toFloat)

    protected fun double(
        default: Double,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Double>> =
        direct(default, encrypted, backup, ::doublePreferencesKey, String::toDouble)

    protected fun bool(
        default: Boolean,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Boolean>> =
        direct(default, encrypted, backup, ::booleanPreferencesKey, String::toBoolean)

    protected fun string(
        default: String,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<String>> =
        direct(default, encrypted, backup, ::stringPreferencesKey) { it }
    //endregion

    //region converted delegates
    protected inline fun <reified T : Any> KDataStore.any(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
        noinline convert: (T) -> String,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            backup = backup,
            actualStore = actualStore,
            backupStore = backupStore,
            getKey = ::stringPreferencesKey,
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
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Byte>> =
        any(default, encrypted, backup, convert = Any::toString, recover = String::toByte)

    protected fun short(
        default: Short,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Short>> =
        any(default, encrypted, backup, convert = Any::toString, recover = String::toShort)

    protected fun char(
        default: Char,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<Char>> =
        any(default, encrypted, backup, convert = Any::toString, recover = String::single)

    protected inline fun <reified T : Enum<T>> enum(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> {
        val valueOf = default::class.functions.first { it.name == "valueOf" }

        return any(
            default = default,
            encrypted = encrypted,
            backup = backup,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> javaSerializable(
        default: T,
        encrypted: Boolean = defaultEncrypted,
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            encrypted = encrypted,
            backup = backup,
            actualStore = actualStore,
            backupStore = backupStore,
            getKey = ::stringPreferencesKey,
            convert = { t ->
                val bos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(bos)
                oos.writeObject(t)
                oos.close()
                bos.close()
                val bytes = bos.toByteArray().updateIf({ encrypted }){ encryption!!.encrypt(it) }
                Base64.encodeToString(bytes, Base64.DEFAULT)
            },
            recover = { src ->
                val bytes = Base64.decode(src, Base64.DEFAULT)
                    .updateIf({ encrypted }){ encryption!!.decrypt(it) }

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
        backup: Boolean = false,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        any(default, encrypted, backup, Json::encodeToString, Json::decodeFromString)
    //endregion
}