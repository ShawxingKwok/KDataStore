package pers.shawxingkwok.kdatastore

import kotlinx.coroutines.flow.MutableStateFlow

public expect interface KDSFlow<T> : MutableStateFlow<T> {
    public fun reset()
}