package pers.shawxingkwok.kdatastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.shawxingkwok.androidutil.AppContext
import pers.shawxingkwok.kdatastore.hidden.MLog
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.allDo
import java.io.File
import java.io.IOException

public actual abstract class KDataStore actual constructor(
    @PublishedApi internal actual val fileName: String,
    @PublishedApi internal actual val cipher: Cipher?,
    @PublishedApi internal actual val handlerScope: CoroutineScope,
    ioScope: CoroutineScope,
) {
    @PublishedApi
    internal val kdsFlows: MutableList<KDSFlow<*>> = mutableListOf<KDSFlow<*>>()

    //region getFile, getBackupFile
    private fun getFile(): File {
        return AppContext.preferencesDataStoreFile(fileName)
    }

    private fun getBackupFile(): File {
        return AppContext.preferencesDataStoreFile("$fileName.bak")
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
                MLog.e("Backup dataStore $fileName.bak corrupted.")
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

            // suppose frontStore corrupted
            if (frontPrefs.asMap().none()) {
                backupPrefs -= ioExceptionRecordsKey
                return@runBlocking backupPrefs
            }

            backupPrefs[ioExceptionRecordsKey]
                ?.map(::stringPreferencesKey)
                ?.forEach { key ->
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

            allDo(frontStore, backupStore) { store ->
                store.edit { prefs ->
                    needlessKeys.forEach { key ->
                        prefs -= key
                    }
                }
            }
        }
    }
    //endregion

    //region reset delete exist
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public actual annotation class CautiousApi actual constructor()

    @CautiousApi
    public actual fun delete() {
        getFile().delete()
        getBackupFile().delete()
    }

    // todo: consider about 's.exists(backupPath)'
    public actual fun exist(): Boolean{
        return getFile().exists()
    }

    @CautiousApi
    public actual fun reset(){
        kdsFlows.forEach { it.reset() }
    }
    //endregion

    //region converted delegates
    @PublishedApi
    internal actual inline fun <reified T> _storeAny(
        default: T,
        noinline convert: (T & Any) -> String,
        noinline recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        FlowDelegate<T>(
            default = default,
            convert =
                if (cipher != null)
                    { t ->
                        val utf8Bytes = convert(t).encodeToByteArray()
                        val base64Bytes = cipher.encrypt(utf8Bytes)
                        Json.encodeToString(ByteArraySerializer(), base64Bytes)
                    }
                else
                    convert,
            recover =
                if (cipher != null) { data: String ->
                    val base64Bytes = Json.decodeFromString(ByteArraySerializer(), data)
                    val utf8Bytes = cipher.decrypt(base64Bytes)
                    String(utf8Bytes).let(recover)
                }
                else
                    recover,
        )

    //todo: switch to stream when 'Json.encodeToStream' is not experimental.
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal actual inline fun <reified T> _storeSerializable(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        when(T::class){
            Boolean::class -> _storeAny(default, Any::toString, String::toBoolean)
            Int::class -> _storeAny(default, Any::toString, String::toInt)
            Long::class -> _storeAny(default, Any::toString, String::toLong)
            Float::class -> _storeAny(default, Any::toString, String::toFloat)
            Double::class -> _storeAny(default, Any::toString, String::toDouble)
            String::class -> _storeAny(default, Any::toString) { it }
            else -> _storeAny(default, Json::encodeToString){ Json.decodeFromString(it) }
        }
        as KReadOnlyProperty<KDataStore, KDSFlow<T>>

    /*
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <reified T: Any, reified S> store(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        _storeAny(
            default = default,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    /*
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <reified T: Any, reified S> storeNullable(
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T?>> =
        _storeAny(
            default = null,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    protected actual inline fun <reified T: Any> store(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        _storeSerializable(default)

    protected actual inline fun <reified T: Any> storeNullable(): KReadOnlyProperty<KDataStore, KDSFlow<T?>> =
        _storeSerializable(null)
    //endregion
}