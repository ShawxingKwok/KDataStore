package pers.apollokwok.kdatastore

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pers.apollokwok.kdatastore.hidden.DefaultIOScope
import pers.shawxingkwok.ktutil.KReadOnlyProperty

/**
 * An extended data store with little configuration, easy encryption, exception safety
 * and extensive supported types.
 *
 * See tutorial in my [ITWorks](https://shawxingkwok.github.io/ITWorks/). It's inside
 * **Android** group in version `1.0.0` and would be moved to **KMM** in version `1.1.0`.
 */
public expect abstract class KDataStore(
    fileName: String,
    cypher: Cypher? = null,
    handlerScope: CoroutineScope = MainScope(),
    ioScope: CoroutineScope = DefaultIOScope,
) {
    internal val fileName: String
    internal val cypher: Cypher?
    internal val handlerScope: CoroutineScope

    public interface Flow<T> : MutableStateFlow<T>{
        public fun reset()
    }

    //region Cautious functions: reset delete exists
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public annotation class CautiousApi()

    @CautiousApi
    public fun delete()

    // todo: consider about including 'backupFile.exists'
    public fun exists(): Boolean

    @CautiousApi
    public fun reset()
    //endregion

    //region converted delegates
    @PublishedApi
    internal inline fun <reified T> _storeAny(
        default: T,
        noinline convert: (T & Any) -> String,
        noinline recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>>

    //todo: switch to stream when 'Json.encodeToStream' is not experimental.
    @PublishedApi
    internal inline fun <reified T> _storeSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>>

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected inline fun <reified T: Any, reified S> store(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>>

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected inline fun <reified T: Any, reified S> storeNullable(
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T?>>

    protected inline fun <reified T: Any> store(default: T): KReadOnlyProperty<KDataStore, Flow<T>>

    protected inline fun <reified T: Any> storeNullable(): KReadOnlyProperty<KDataStore, Flow<T?>>
    //endregion
}