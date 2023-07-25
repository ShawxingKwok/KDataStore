package pers.shawxingkwok.kdatastore

import kotlinx.coroutines.*
import pers.shawxingkwok.kdatastore.hidden.DefaultIOScope
import pers.shawxingkwok.ktutil.KReadOnlyProperty

/**
 * An extended data store with little configuration, easy crypto, exception safety
 * and extensive supported types.
 *
 * See [document](https://shawxingkwok.github.io/ITWorks/docs/android/kdatastore/en/) which
 * would be moved to the new [page](https://shawxingkwok.github.io/ITWorks/docs/multiplatform-mobile/kdatastore/en/)
 * since version `1.1.0`.
 */
public expect abstract class KDataStore(
    fileName: String,
    cipher: Cipher? = null,
    handlerScope: CoroutineScope = MainScope(),
    ioScope: CoroutineScope = DefaultIOScope,
) {
    internal val fileName: String
    internal val cipher: Cipher?
    internal val handlerScope: CoroutineScope

    //region reset delete exist
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class CautiousApi()

    @CautiousApi
    public fun delete()

    // todo: consider including 'backupFile.exists'
    public fun exist(): Boolean

    internal var resetCalled: Boolean
        private set

    @CautiousApi
    public fun reset()
    //endregion

    //region converted delegates
    @PublishedApi
    internal fun <T> _store(
        default: T,
        convert: (T & Any) -> String,
        recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>>

    //todo: switch to stream when 'Json.encodeToStream' is supported.
    @PublishedApi
    internal inline fun <reified T> _store(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>>

    protected inline fun <T: Any, reified S> store(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T>>

    protected inline fun <T: Any, reified S> storeNullable(
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, KDSFlow<T?>>

    protected inline fun <reified T: Any> store(default: T): KReadOnlyProperty<KDataStore, KDSFlow<T>>

    protected inline fun <reified T: Any> storeNullable(): KReadOnlyProperty<KDataStore, KDSFlow<T?>>
    //endregion
}