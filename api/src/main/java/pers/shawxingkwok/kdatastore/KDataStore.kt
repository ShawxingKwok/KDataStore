package pers.shawxingkwok.kdatastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.allDo
import java.io.*
import kotlin.reflect.full.functions

/**
 * An extended data store with little configuration, easy encryption and extensive supported types.
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

    @PublishedApi
    internal val flows: MutableList<Flow<*>> = mutableListOf()

    protected val appContext: Context = MyInitializer.context

    //region getFile, getBackupFile
    private fun getFile(): File {
        return appContext.preferencesDataStoreFile(fileName)
    }

    private fun getBackupFile(): File {
        return appContext.preferencesDataStoreFile("${fileName}.bak")
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
    private inline fun <reified T> KDataStore.direct(
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

    protected fun int(default: Int): KReadOnlyProperty<KDataStore, Flow<Int>> =
        direct(default, ::intPreferencesKey, String::toInt)

    protected fun nullableInt(): KReadOnlyProperty<KDataStore, Flow<Int?>> =
        direct(null, ::intPreferencesKey, String::toInt)

    protected fun long(default: Long): KReadOnlyProperty<KDataStore, Flow<Long>> =
        direct(default, ::longPreferencesKey, String::toLong)

    protected fun nullableLong(): KReadOnlyProperty<KDataStore, Flow<Long?>> =
        direct(null, ::longPreferencesKey, String::toLong)

    protected fun float(default: Float): KReadOnlyProperty<KDataStore, Flow<Float>> =
        direct(default, ::floatPreferencesKey, String::toFloat)

    protected fun nullableFloat(): KReadOnlyProperty<KDataStore, Flow<Float?>> =
        direct(null, ::floatPreferencesKey, String::toFloat)

    protected fun double(default: Double): KReadOnlyProperty<KDataStore, Flow<Double>> =
        direct(default, ::doublePreferencesKey, String::toDouble)

    protected fun nullableDouble(): KReadOnlyProperty<KDataStore, Flow<Double?>> =
        direct(null, ::doublePreferencesKey, String::toDouble)

    protected fun bool(default: Boolean): KReadOnlyProperty<KDataStore, Flow<Boolean>> =
        direct(default, ::booleanPreferencesKey, String::toBoolean)

    protected fun nullableBool(): KReadOnlyProperty<KDataStore, Flow<Boolean?>> =
        direct(null, ::booleanPreferencesKey, String::toBoolean)

    protected fun string(default: String): KReadOnlyProperty<KDataStore, Flow<String>> =
        direct(default, ::stringPreferencesKey) { it }

    protected fun nullableString(): KReadOnlyProperty<KDataStore, Flow<String?>> =
        direct(null, ::stringPreferencesKey) { it }
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

    protected inline fun <reified T : Enum<T>> enum(default: T): KReadOnlyProperty<KDataStore, Flow<T>> {
        val valueOf = T::class.functions.first { it.name == "valueOf" }

        return anyWithString<T>(
            default = default,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Enum<T>> nullableEnum(): KReadOnlyProperty<KDataStore, Flow<T?>> {
        val valueOf = T::class.functions.first { it.name == "valueOf" }

        return anyWithString<T?>(
            default = null,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> javaSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        FlowDelegate<T>(
            default = default,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable(cypher) },
        )

    protected inline fun <reified T : Serializable> nullableJavaSerializable(): KReadOnlyProperty<KDataStore, Flow<T?>> =
        FlowDelegate<T?>(
            default = null,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable(cypher) },
        )

    protected inline fun <reified T: Any> ktSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString(default, Json::encodeToString, Json::decodeFromString)

    protected inline fun <reified T: Any> nullableKtSerializable(): KReadOnlyProperty<KDataStore, Flow<T?>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString<T?>(null, Json::encodeToString, Json::decodeFromString)

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [Serializable];
     */
    protected inline fun <reified T: Any, reified S: Serializable> any(
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
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [Serializable];
     */
    protected inline fun <reified T: Any, reified S: Serializable> nullableAny(
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