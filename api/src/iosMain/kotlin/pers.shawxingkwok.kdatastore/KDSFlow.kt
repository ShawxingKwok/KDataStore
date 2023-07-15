package pers.shawxingkwok.kdatastore

import kotlinx.coroutines.flow.MutableStateFlow

public actual interface KDSFlow<T> : MutableStateFlow<T> {
    public actual fun reset()
}