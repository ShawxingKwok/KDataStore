package pers.shawxingkwok.kdatastore

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.MutableStateFlow

public actual interface KDSFlow<T> : MutableStateFlow<T> {
    public actual fun reset()
    public val liveData: LiveData<T>
}