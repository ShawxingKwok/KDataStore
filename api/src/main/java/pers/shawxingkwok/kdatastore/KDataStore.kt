package pers.shawxingkwok.kdatastore

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.LiveData
import androidx.startup.Initializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.allDo
import java.io.*
import kotlin.concurrent.thread
import kotlin.reflect.full.functions

/**
 * An extended data store with little configuration, easy encryption, exception safety and extensive supported types.
 *
 * See [tutorial](https://shawxingkwok.github.io/ITWorks/docs/kdatastore/).
 */
public abstract class KDataStore(
    @PublishedApi internal val fileName: String,
    @PublishedApi internal val cypher: Cypher? = null,
    @PublishedApi internal val handlerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    public interface Flow<T> : MutableStateFlow<T>{
        public fun reset()

        /**
         * Returns the only one converted [LiveData].
         */
        public val liveData: LiveData<T>
    }

    @SuppressLint("EnsureInitializerNoArgConstr")
    public abstract class Initializer(
        private val act: () -> Unit
    )
        : androidx.startup.Initializer<Unit>
    {
        final override fun dependencies(): List<Class<out androidx.startup.Initializer<*>>> {
            return emptyList()
        }

        final override fun create(context: Context) {
            thread { act() }
        }
    }

    @PublishedApi
    internal val flows: MutableList<Flow<*>> = mutableListOf()

    protected val appContext: Context = MyInitializer.context

    //region getFile, getBackupFile
    private fun getFile(): File {
        return appContext.preferencesDataStoreFile(fileName)
    }

    private fun getBackupFile(): File {
        return appContext.preferencesDataStoreFile("$fileName.bak")
    }
    //endregion

    //region frontStore, backupStore
    @PublishedApi
    internal val frontStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                MLog.e("DataStore $fileName corrupted.")
                emptyPreferences()
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = ::getFile,
        )

    @PublishedApi
    internal val backupStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                MLog.e("Backup dataStore $fileName corrupted.")
                emptyPreferences()
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = ::getBackupFile,
        )
    //endregion

    //region get initial preferences and fix
    @PublishedApi
    internal val keys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    // only saved in backup store, because if you write again at once when IOException occurs, IOException
    // would probably occur again.
    @PublishedApi
    internal val ioExceptionRecordsKey: Preferences.Key<Set<String>> =
        stringSetPreferencesKey("ioExceptionRecords$#KDataStore").also { keys += it }

    // recovers with ioException records
    // ignores the IOException because reads only once.
    @PublishedApi
    internal val initialPrefs: Preferences =
        runBlocking {
            val (frontPrefs, backupPrefs) =
                listOf(frontStore, backupStore)
                .map {
                    try {
                        it.data.first()
                    } catch (e: IOException) {
                        emptyPreferences()
                    }
                    .toMutablePreferences()
                }

            if (frontPrefs.asMap().none()) {
                backupPrefs -= ioExceptionRecordsKey
                return@runBlocking backupPrefs
            }

            if (backupPrefs.asMap().none())
                return@runBlocking emptyPreferences()

            backupPrefs[ioExceptionRecordsKey]
                ?.map {
                    val (typeName, propName) = it.split(" ", limit = 2)

                    when (typeName) {
                        "Int" -> intPreferencesKey(propName)
                        "Long" -> longPreferencesKey(propName)
                        "Float" -> floatPreferencesKey(propName)
                        "Double" -> doublePreferencesKey(propName)
                        "Bool" -> floatPreferencesKey(propName)
                        "String" -> floatPreferencesKey(propName)
                        else -> error("")
                    }
                }
                ?.forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    key as Preferences.Key<Any>

                    when (val value = backupPrefs[key]) {
                        null -> frontPrefs -= key
                        else -> frontPrefs[key] = value
                    }
                }

            frontPrefs
        }

    // update disk asyncly
    init {
        handlerScope.launch {
            frontStore.updateData { initialPrefs }
            backupStore.updateData { initialPrefs }
        }
    }

    // remove needless keys later
    init {
        handlerScope.launch {
            delay(1000)

            val needlessKeys = initialPrefs.asMap().keys - keys
            if (needlessKeys.none()) return@launch

            allDo(frontStore, backupStore){ store ->
                store.edit { prefs ->
                    needlessKeys.forEach { key ->
                        prefs -= key
                    }
                }
            }
        }
    }
    //endregion

    //region delicate functions: reset delete exists
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class DelicateApi

    @DelicateApi
    public fun reset() {
        flows.forEach { it.reset() }
    }

    @DelicateApi
    public fun delete() {
        getFile().delete()
        getBackupFile().delete()
    }

    // todo: consider about 'getBackupFile().exists()'
    public fun exists(): Boolean = getFile().exists()
    //endregion

    //region direct delegates
    private inline fun <reified T> KDataStore.directStore(
        default: T,
        noinline getKey: (String) -> Preferences.Key<T & Any>,
        noinline recover: (String) -> T & Any,
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
            anyWithString<T>(
                default = default,
                convert = Any::toString,
                recover = recover,
            )

    protected fun storeInt(default: Int): KReadOnlyProperty<KDataStore, Flow<Int>> =
        directStore(default, ::intPreferencesKey, String::toInt)

    protected fun storeNullableInt(): KReadOnlyProperty<KDataStore, Flow<Int?>> =
        directStore(null, ::intPreferencesKey, String::toInt)

    protected fun storeLong(default: Long): KReadOnlyProperty<KDataStore, Flow<Long>> =
        directStore(default, ::longPreferencesKey, String::toLong)

    protected fun storeNullableLong(): KReadOnlyProperty<KDataStore, Flow<Long?>> =
        directStore(null, ::longPreferencesKey, String::toLong)

    protected fun storeFloat(default: Float): KReadOnlyProperty<KDataStore, Flow<Float>> =
        directStore(default, ::floatPreferencesKey, String::toFloat)

    protected fun storeNullableFloat(): KReadOnlyProperty<KDataStore, Flow<Float?>> =
        directStore(null, ::floatPreferencesKey, String::toFloat)

    protected fun storeDouble(default: Double): KReadOnlyProperty<KDataStore, Flow<Double>> =
        directStore(default, ::doublePreferencesKey, String::toDouble)

    protected fun storeNullableDouble(): KReadOnlyProperty<KDataStore, Flow<Double?>> =
        directStore(null, ::doublePreferencesKey, String::toDouble)

    protected fun storeBool(default: Boolean): KReadOnlyProperty<KDataStore, Flow<Boolean>> =
        directStore(default, ::booleanPreferencesKey, String::toBoolean)

    protected fun storeNullableBool(): KReadOnlyProperty<KDataStore, Flow<Boolean?>> =
        directStore(null, ::booleanPreferencesKey, String::toBoolean)

    protected fun storeString(default: String): KReadOnlyProperty<KDataStore, Flow<String>> =
        directStore(default, ::stringPreferencesKey) { it }

    protected fun storeNullableString(): KReadOnlyProperty<KDataStore, Flow<String?>> =
        directStore(null, ::stringPreferencesKey) { it }
    //endregion

    //region converted delegates
    @PublishedApi
    internal inline fun <reified T> KDataStore.anyWithString(
        default: T,
        noinline convert: (T & Any) -> String,
        noinline recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate<T>(
            default = default,
            getKey = ::stringPreferencesKey,
            convert =
                if (cypher != null)
                    { t ->
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

    protected inline fun <reified T : Enum<T>> storeEnum(default: T): KReadOnlyProperty<KDataStore, Flow<T>> {
        val valueOf = T::class.functions.first { it.name == "valueOf" }

        return anyWithString<T>(
            default = default,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Enum<T>> storeNullableEnum(): KReadOnlyProperty<KDataStore, Flow<T?>> {
        val valueOf = T::class.functions.first { it.name == "valueOf" }

        return anyWithString<T?>(
            default = null,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> storeJavaSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate<T>(
            default = default,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable(cypher) },
        )

    protected inline fun <reified T : Serializable> storeNullableJavaSerializable(): KReadOnlyProperty<KDataStore, Flow<T?>> =
        FlowDelegate<T?>(
            default = null,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable(cypher) },
        )

    protected inline fun <reified T: Any> storeKtSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString(default, Json::encodeToString, Json::decodeFromString)

    protected inline fun <reified T: Any> storeNullableKtSerializable(): KReadOnlyProperty<KDataStore, Flow<T?>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString<T?>(null, Json::encodeToString, Json::decodeFromString)

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected inline fun <reified T: Any, reified S: Serializable> storeAny(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate<T>(
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

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected inline fun <reified T: Any, reified S: Serializable> storeNullableAny(
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T?>> =
        FlowDelegate<T?>(
            default = null,
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