package pers.shawxingkwok.kdatastore

import android.util.Base64
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
import pers.shawxingkwok.ktutil.allDo
import java.io.*
import kotlin.reflect.full.functions

/**
 * An extended data store with little configuration, easy encryption and extensive supported types.
 *
 * See [tutorial](https://shawxingkwok.github.io/ITWorks/docs/kdatastore/).
 */
public abstract class KDataStore(
    private val fileName: String = "settings",
    @PublishedApi internal val handlerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    @PublishedApi internal val cypher: Cypher? = null,
) {
    //region frontStore, backupStore
    private var frontStoreCorrupted = false
    private var backupStoreCorrupted = false

    @PublishedApi
    internal val frontStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                MLog.e("DataStore $fileName corrupted.")

                frontStoreCorrupted = true

                //todo: replace 'runBlocking' with 'suspend' after the official fix
                runBlocking {
                    backupStore.data.first()
                }
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName)
            }
        )

    @PublishedApi
    internal val backupStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                MLog.e("Backup dataStore $fileName corrupted.")
                backupStoreCorrupted = true
                emptyPreferences()
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = {
                MyInitializer.context.preferencesDataStoreFile(fileName + "_backup")
            }
        )
    //endregion

    //region get initial preferences and fix
    @PublishedApi
    internal val keys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    @PublishedApi
    internal val ioExceptionRecordsKey: Preferences.Key<Set<String>> =
        stringSetPreferencesKey("ioExceptionRecords$#KDataStore").also { keys += it }

    @PublishedApi
    internal val initialPrefs: Preferences =
        runBlocking {
            frontStore.data.first().toMutablePreferences()
        }

    // recovers with ioException records
    // ignores the IOException because reads only once.
    init {
        runBlocking{
            initialPrefs as MutablePreferences

            when{
                frontStoreCorrupted && backupStoreCorrupted -> {
                    // used emptyPreferences in corruption
                    return@runBlocking
                }
                frontStoreCorrupted -> {
                    // used backup preferences in corruption
                    return@runBlocking
                }
                backupStoreCorrupted -> {
                    // used emptyPreferences in corruption first
                    // and update with frontPreferences later in this block
                }
                else -> {
                    val ioExceptionKeysInfo = initialPrefs[ioExceptionRecordsKey]
                    // emptySet is impossible
                    if (ioExceptionKeysInfo != null) {
                        val backupPrefs = backupStore.data.first()

                        ioExceptionKeysInfo
                            .map {
                                val (typeName, propName) = it.split(" ", limit = 2)
                                when(typeName){
                                    "Int" -> intPreferencesKey(propName)
                                    "Long" -> longPreferencesKey(propName)
                                    "Float" -> floatPreferencesKey(propName)
                                    "Double" -> doublePreferencesKey(propName)
                                    "Bool" -> floatPreferencesKey(propName)
                                    "String" -> floatPreferencesKey(propName)
                                    else -> error("")
                                }
                            }
                            .forEach { key ->
                                @Suppress("UNCHECKED_CAST")
                                key as Preferences.Key<Any>

                                when (val value = backupPrefs[key]) {
                                    null -> initialPrefs -= key
                                    else -> initialPrefs[key] = value
                                }
                            }
                    }
                }
            }

            initialPrefs -= ioExceptionRecordsKey

            // update disk asyncly
            handlerScope.launch {
                frontStore.updateData { initialPrefs }
                backupStore.updateData { initialPrefs }
            }
        }
    }

    // remove needless keys
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
    public suspend fun reset() {
        frontStore.updateData { emptyPreferences() }
        backupStore.updateData { emptyPreferences() }
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
    : KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
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

    protected fun int(default: Int): KReadOnlyProperty<KDataStore, MutableStateFlow<Int>> =
        direct(default, ::intPreferencesKey, String::toInt)

    protected fun long(default: Long): KReadOnlyProperty<KDataStore, MutableStateFlow<Long>> =
        direct(default, ::longPreferencesKey, String::toLong)

    protected fun float(default: Float): KReadOnlyProperty<KDataStore, MutableStateFlow<Float>> =
        direct(default, ::floatPreferencesKey, String::toFloat)

    protected fun double(default: Double): KReadOnlyProperty<KDataStore, MutableStateFlow<Double>> =
        direct(default, ::doublePreferencesKey, String::toDouble)

    protected fun bool(default: Boolean): KReadOnlyProperty<KDataStore, MutableStateFlow<Boolean>> =
        direct(default, ::booleanPreferencesKey, String::toBoolean)

    protected fun string(default: String): KReadOnlyProperty<KDataStore, MutableStateFlow<String>> =
        direct(default, ::stringPreferencesKey) { it }
    //endregion

    //region converted delegates
    @PublishedApi
    internal inline fun <reified T : Any> KDataStore.anyWithString(
        default: T,
        noinline convert: (T) -> String,
        noinline recover: (String) -> T,
    )
    : KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
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

    protected inline fun <reified T : Enum<T>> enum(default: T): KReadOnlyProperty<KDataStore, MutableStateFlow<T>> {
        val valueOf = default::class.functions.first { it.name == "valueOf" }

        return anyWithString(
            default = default,
            convert = Any::toString,
            recover = {
                valueOf.call(it)!! as T
            }
        )
    }

    protected inline fun <reified T : Serializable> javaSerializable(default: T): KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
        FlowDelegate(
            default = default,
            getKey = ::stringPreferencesKey,
            convert = { t -> t.convertToString(cypher) },
            recover = { src -> src.recoverToSerializable<T>(cypher) },
        )

    protected inline fun <reified T : Any> ktSerializable(default: T): KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
        //todo: switch to stream when 'Json.encodeToStream' is not experimental.
        anyWithString(default, Json::encodeToString, Json::decodeFromString)

    protected inline fun <reified T : Any, reified S: Serializable> any(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, MutableStateFlow<T>> =
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