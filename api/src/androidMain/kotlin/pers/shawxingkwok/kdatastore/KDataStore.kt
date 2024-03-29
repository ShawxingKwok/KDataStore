package pers.shawxingkwok.kdatastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.shawxingkwok.kdatastore.hidden.MLog
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import pers.shawxingkwok.ktutil.allDo
import java.io.File
import java.io.IOException

/**
 * An extended data store with little configuration, easy crypto, exception safety
 * and extensive supported types.
 *
 * See [document](https://shawxingkwok.github.io/ITWorks/docs/android/kdatastore/en/) which
 * would be moved to the new [page](https://shawxingkwok.github.io/ITWorks/docs/multiplatform/kdatastore/en/)
 * since version `1.1.0`.
 */
public actual abstract class KDataStore actual constructor(
    internal actual val fileName: String,
    internal actual val cipher: Cipher?,
    internal actual val handlerScope: CoroutineScope,
    internal actual val ioScope: CoroutineScope,
) {
    internal val kdsFlows: MutableList<KDSFlow<*>> = mutableListOf()

    protected val appContext: Context =
        try {
            KDataStoreInitializer.context
        }catch (e: NullPointerException){
            throw NullPointerException(
                "${e.message}. Include KDataStoreInitializer::class.java in dependencies in that caller module, " +
                "which may be indirect, with startup-runtime."
            )
        }

    internal fun getIOExceptionTagFile(): File{
        return File(KDataStoreInitializer.context.filesDir, "datastore/$fileName.corruptionTag")
    }

    //region getFile, getBackupFile
    private fun getFile(): File {
        return KDataStoreInitializer.context.preferencesDataStoreFile(fileName)
    }

    private fun getBackupFile(): File {
        return KDataStoreInitializer.context.preferencesDataStoreFile("$fileName.bak")
    }
    //endregion

    private val hasIOExceptionBefore = getIOExceptionTagFile().exists()

    private var corrupted = false

    //region frontStore, backupStore
    internal val frontStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                corrupted = true
                MLog.d("Front dataStore $fileName corrupted.")
                runBlocking { backupStore.data.first() }
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = ::getFile,
        )

    internal val backupStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                MLog.d("Backup dataStore $fileName corrupted.")
                if (corrupted)
                    emptyPreferences()
                else
                    runBlocking { frontStore.data.first() }
            },
            migrations = emptyList(),
            scope = ioScope,
            produceFile = ::getBackupFile,
        )
    //endregion

    //region get initial preferences and fix
    internal val keys: MutableSet<Preferences.Key<*>> = mutableSetOf()

    // only saved in backup store, because if you write again at once when IOException occurs, IOException
    // would probably occur again.
    internal val ioExceptionRecordsKey: Preferences.Key<Set<String>> =
        stringSetPreferencesKey("ioExceptionRecords")

    internal var initialCorrectingJob: Job? = null

    // recovers with ioException records
    // ignores the IOException because reads only once.
    internal val initialPrefs: Preferences =
        runBlocking {
            val frontPrefs =
                try {
                    frontStore.data.first().toMutablePreferences()
                }catch (e: IOException){
                    MLog.e("Datastore $fileName encountered IOException.")
                    null
                }

            if (frontPrefs != null && !hasIOExceptionBefore) {
                // remove needless keys later if any
                handlerScope.launch {
                    delay(1000)

                    val needlessKeys = frontPrefs.asMap().keys - keys
                    if (needlessKeys.none()) return@launch

                    // Here is rarely invoked. IOException is thrown out.
                    allDo(frontStore, backupStore) { store ->
                        store.edit { prefs ->
                            needlessKeys.forEach { prefs -= it }
                        }
                    }
                }
                return@runBlocking frontPrefs
            }

            // Handles IOException
            MLog.d("Start to handle IOException.")

            val backupPrefs =
                try {
                    backupStore.data.first()
                }catch (e: IOException){
                    MLog.e("Backup datastore $fileName encountered IOException.")
                    null
                }

            return@runBlocking when{
                frontPrefs == null && backupPrefs == null ->
                    error("Unexpected IOExceptions for both datastore $fileName and its backup.")

                // If there is writing IOException record and just encountered reading IOException,
                // lay the IOException records aside, which rarely occurs.
                frontPrefs == null -> backupPrefs!!
                backupPrefs == null -> frontPrefs

                else -> {
                    backupPrefs[ioExceptionRecordsKey]
                        ?.map(::stringPreferencesKey)
                        ?.forEach { key ->
                            val value = backupPrefs[key]

                            when(value) {
                                null -> frontPrefs -= key
                                else -> frontPrefs[key] = value
                            }

                            MLog.d("Update ${key.name.decryptIfNeeded(cipher)} " +
                                    "with ${value?.decryptIfNeeded(cipher) ?: "default"} " +
                                    "from the backup datastore."
                            )
                        }

                    // Here is rarely invoked. IOException is thrown out.
                    initialCorrectingJob = handlerScope.launch {
                        frontStore.updateData { frontPrefs }
                        backupStore.updateData { frontPrefs }
                        ioScope.launch {
                            getIOExceptionTagFile().delete()
                        }
                    }

                    frontPrefs
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

    internal actual var resetCalled = false
        private set

    @CautiousApi
    public actual fun reset(){
        resetCalled = true
        kdsFlows.forEach { it.reset() }
    }
    //endregion

    //region converted delegates
    @PublishedApi
    internal actual fun <T> _store(
        default: T,
        convert: (T & Any) -> String,
        recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        KDSFlowDelegate(
            default = default,
            convert =
                if (cipher != null)
                    { t -> convert(t).encryptIfNeeded(cipher) }
                else
                    convert,
            recover =
                if (cipher != null) { data: String ->
                    data.decryptIfNeeded(cipher).let(recover)
                } else
                    recover,
        )

    //todo: switch to stream when 'Json.encodeToStream' is not experimental.
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal actual inline fun <reified T> _store(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        when(T::class){
            Boolean::class -> _store(default, Any::toString, String::toBoolean)
            Int::class -> _store(default, Any::toString, String::toInt)
            Long::class -> _store(default, Any::toString, String::toLong)
            Float::class -> _store(default, Any::toString, String::toFloat)
            Double::class -> _store(default, Any::toString, String::toDouble)
            String::class -> _store(default, Any::toString) { it }
            else -> _store(default, Json::encodeToString){ Json.decodeFromString(it) }
        }
        as KReadOnlyProperty<KDataStore, KDSFlow<T>>

    /*
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <T: Any, reified S: Any> store(
        default: T,
        crossinline convert: (T) -> S,
        crossinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        _store(
            default = default,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    /*
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <T: Any, reified S: Any> storeNullable(
        crossinline convert: (T) -> S,
        crossinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T?>> =
        _store(
            default = null,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    protected actual inline fun <reified T: Any> store(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>> =
        _store(default)

    protected actual inline fun <reified T: Any> storeNullable(): KReadOnlyProperty<KDataStore, KDSFlow<T?>> =
        _store(null)
    //endregion
}