package pers.apollokwok.kdatastore

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pers.apollokwok.kdatastore.hidden.*
import pers.shawxingkwok.ktutil.KReadOnlyProperty

public actual abstract class KDataStore actual constructor(
    @PublishedApi internal actual val fileName: String,
    @PublishedApi internal actual val cypher: Cypher?,
    @PublishedApi internal actual val handlerScope: CoroutineScope,
    ioScope: CoroutineScope,
) {
    public actual interface Flow<T> : MutableStateFlow<T>{
        public actual fun reset()
    }

    //region delicate functions: reset delete exists
    @RequiresOptIn
    @Retention(AnnotationRetention.BINARY)
    public actual annotation class CautiousApi actual constructor()

    public actual fun delete() {
        TODO()
    }

    // todo: consider about 's.exists(backupPath)'
    public actual fun exists(): Boolean{
        TODO()
    }

    public actual fun reset(){
        TODO()
    }
    //endregion

    //region converted delegates
    @PublishedApi
    internal actual inline fun <reified T> _storeAny(
        default: T,
        noinline convert: (T & Any) -> String,
        noinline recover: (String) -> T & Any,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> = TODO()

    //todo: switch to stream when 'Json.encodeToStream' is not experimental.
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal actual inline fun <reified T> _storeSerializable(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        when(T::class){
            Boolean::class -> _storeAny(default, Any::toString, String::toBoolean)
            Int::class -> _storeAny(default, Any::toString, String::toInt)
            Long::class -> _storeAny(default, Any::toString, String::toLong)
            Float::class -> _storeAny(default, Any::toString, String::toFloat)
            Double::class -> _storeAny(default, Any::toString, String::toDouble)
            else -> _storeAny(default, Json::encodeToString){ Json.decodeFromString(it) }
        }
        as KReadOnlyProperty<KDataStore, Flow<T>>

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <reified T: Any, reified S> store(
        default: T,
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T>> =
        _storeAny(
            default = default,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    /**
     * I suggest you convert data to [Pair], [Triple], [List] or other convenient containers of [S];
     */
    protected actual inline fun <reified T: Any, reified S> storeNullable(
        noinline convert: (T) -> S,
        noinline recover: (S) -> T,
    )
    : KReadOnlyProperty<KDataStore, Flow<T?>> =
        _storeAny(
            default = null,
            convert = { Json.encodeToString(convert(it)) },
            recover = { Json.decodeFromString<S>(it).let(recover) }
        )

    protected actual inline fun <reified T: Any> store(default: T): KReadOnlyProperty<KDataStore, Flow<T>> =
        _storeSerializable(default)

    protected actual inline fun <reified T: Any> storeNullable(): KReadOnlyProperty<KDataStore, Flow<T?>> =
        _storeSerializable<T?>(null)
    //endregion
}