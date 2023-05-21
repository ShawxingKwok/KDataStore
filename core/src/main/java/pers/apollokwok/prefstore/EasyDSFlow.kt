package pers.apollokwok.prefstore

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

public abstract class EasyDSFlow<T: Any> : Flow<T>{
    private companion object{
        val scope = MainScope()
    }

    public abstract val default: T

    public abstract suspend fun emit(value: T)

    /**
     * [transform]s the old value and emit it.
     */
    public suspend fun emit(transform: (T) -> T){
        val value = transform(first())
        emit(value)
    }

    /**
     * Emits [value] in an async way.
     */
    public fun toss(value: T){
        scope.launch { emit(value) }
    }

    /**
     * [transform]s the old value and emits it in an async way.
     */
    public fun toss(transform: (T) -> T){
        scope.launch { emit(transform) }
    }
}