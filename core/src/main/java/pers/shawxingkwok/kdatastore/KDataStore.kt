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
import java.io.*
import kotlin.reflect.full.functions

/**
 * An extended data store with little configuration, easy encryption and extensive supported types.
 *
 * See [tutorial](https://shawxingkwok.github.io/ITWorks/docs/kdatastore/).
 */
public abstract class KDataStore(
    private val fileName: String = "settings",
    @PublishedApi internal val castScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    @PublishedApi internal val cypher: Cypher? = null,
) {
    //region Flow
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
        public fun cast(value: T) {
            handlerScope.launch { emit(value) }
        }

        /**
         * [transform]s the old value and emits it in an async way.
         */
        public fun cast(transform: (T) -> T) {
            handlerScope.launch { emit(transform) }
        }
    }
    //endregion

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
        backup.update(updatedAllPrefs!!)
        updatedAllPrefs = null
    }
    //endregion

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
                backup.update(migratedPrefs!!)
                return migratedPrefs!!
            }

            override suspend fun cleanUp() {
                migration.cleanUp()
                migratedPrefs = null
            }
        }
    //endregion

    @PublishedApi
    internal val keys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    @PublishedApi
    internal val fixMigration: DataMigration<Preferences> = object : DataMigration<Preferences> {

        lateinit var needlessKeys: Set<Preferences.Key<*>>

        override suspend fun shouldMigrate(currentData: Preferences): Boolean {
            needlessKeys = currentData.asMap().keys - keys
            return needlessKeys.any()
        }

        override suspend fun migrate(currentData: Preferences): Preferences {
            val prefs = currentData.toMutablePreferences()
            needlessKeys.forEach { prefs -= it }
            return prefs
        }

        override suspend fun cleanUp() {}
    }

    //region actualStore
    @PublishedApi
    internal val actualStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                //todo: replace 'runBlocking' with 'suspend' after the official fix
                runBlocking {
                    backup.getPreferences()
                }
            },
            migrations = listOf(defaultDataMigration, fixMigration),
            scope = ioScope,
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName)
            }
        )
    //endregion

    //region backup
    @PublishedApi
    internal inner class Backup{
        private val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(fixMigration),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName + "_backup")
            }
        )

        private val backupHandlerScope: CoroutineScope =
            CoroutineScope(Dispatchers.Default + SupervisorJob())

        suspend fun getPreferences() = dataStore
            .data
            .catch {
                if (it is IOException)
                    emit(emptyPreferences())
                else {
                    it.printStackTrace()
                    throw it
                }
            }
            .first()

        fun update(preferences: Preferences){
            backupHandlerScope.launch {
                try {
                    dataStore.updateData { preferences }
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }

        fun edit(act: suspend (MutablePreferences) -> Unit){
            backupHandlerScope.launch {
                try {
                    dataStore.edit(act)
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    @PublishedApi
    internal val backup: Backup = Backup()
    //endregion

    @PublishedApi
    internal val caughtData: kotlinx.coroutines.flow.Flow<Preferences> =
        actualStore.data
        .catch { tr ->
            if (tr !is IOException) throw tr
            tr.printStackTrace()
            emit(backup.getPreferences())
        }

    //region delicate functions: reset delete exists
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class DelicateApi

    @DelicateApi
    public suspend fun reset() {
        actualStore.updateData { emptyPreferences() }
        backup.update(emptyPreferences())
    }

    private fun getFile() = File(MyInitializer.context.filesDir, "datastore/$fileName.preferences_pb")
    private fun getBackupFile() = File(MyInitializer.context.filesDir, "datastore/${fileName}_backup.preferences_pb")

    @DelicateApi
    public fun delete(): Boolean {
        return getFile().delete() && getBackupFile().delete()
    }

    // todo: consider about 'getBackupFile().exists()'
    public fun exists(): Boolean {
        return getFile().exists()
    }
    //endregion

    //region direct delegates
    private inline fun <reified T : Any> KDataStore.direct(
        default: T,
        noinline getKey: (String) -> Preferences.Key<T>,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        if (cypher == null)
            FlowDelegate(
                default = default,
                getKey = getKey,
                convert = null,
                recover = null,
            )
        else
            anyWithString(
                default = default,
                convert = Any::toString,
                recover = recover,
            )

    protected fun int(default: Int): KReadOnlyProperty<KDataStore, Flow<Int>> =
        direct(default, ::intPreferencesKey, String::toInt)

    protected fun long(default: Long): KReadOnlyProperty<KDataStore, Flow<Long>> =
        direct(default, ::longPreferencesKey, String::toLong)

    protected fun float(default: Float): KReadOnlyProperty<KDataStore, Flow<Float>> =
        direct(default, ::floatPreferencesKey, String::toFloat)

    protected fun double(default: Double): KReadOnlyProperty<KDataStore, Flow<Double>> =
        direct(default, ::doublePreferencesKey, String::toDouble)

    protected fun bool(default: Boolean): KReadOnlyProperty<KDataStore, Flow<Boolean>> =
        direct(default, ::booleanPreferencesKey, String::toBoolean)

    protected fun string(default: String): KReadOnlyProperty<KDataStore, Flow<String>> =
        direct(default, ::stringPreferencesKey) { it }
    //endregion

    //region converted delegates
    @PublishedApi
    internal inline fun <reified T : Any> KDataStore.anyWithString(
        default: T,
        noinline convert: (T) -> String,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            getKey = ::stringPreferencesKey,
            convert =
                if (cypher != null)
                    { t: T ->
                        convert(t)
                        .toByteArray()
                        .let(cypher::encrypt)
                        .let { Base64.encodeToString(it, Base64.DEFAULT) }
                    }
                else
                    convert,
            recover =
                if (cypher != null) { data: String ->
                    Base64.decode(data, Base64.DEFAULT)
                        .let(cypher::decrypt)
                        .let(::String)
                        .let(recover)
                }
                else
                    recover,
        )

    protected inline fun <reified T : Enum<T>> enum(default: T): KReadOnlyProperty<KDataStore, Flow<T>> {
        val valueOf = default::class.functions.first { it.name == "valueOf" }

        return anyWithString(
            default = default,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> javaSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable<T>(cypher) },
        )

    protected inline fun <reified T : Any> ktSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString(default, Json::encodeToString, Json::decodeFromString)

    protected inline fun <reified T : Any, reified S: Serializable> any(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate(
            default = default,
            getKey = ::stringPreferencesKey,
            convert = { t ->
                val s = convert(t)
                s.convertToString(cypher)
            },
            recover = { src ->
                src.recoverToSerializable<S>(cypher)
                .let(recover)
            },
        )
    //endregion
}