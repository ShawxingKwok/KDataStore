package pers.apollokwok.kdatastore

import androidx.lifecycle.MutableLiveData
import pers.shawxingkwok.ktutil.KReadOnlyProperty
import kotlin.reflect.KProperty

public fun <T> KReadOnlyProperty<KDataStore, KDataStore.Flow<T>>.asMutableLiveData()
    : KReadOnlyProperty<KDataStore, MutableLiveData<T>> =
object : KReadOnlyProperty<KDataStore, MutableLiveData<T>> {

    lateinit var liveData: MutableLiveData<T>

    override fun onDelegate(thisRef: KDataStore, property: KProperty<*>) {
        this@asMutableLiveData.onDelegate(thisRef, property)

        val flow = this@asMutableLiveData.getValue(thisRef, property)

        liveData = MutableLiveData(flow.value)

        liveData.observeForever {
            flow.value = it
        }
    }

    override fun getValue(thisRef: KDataStore, property: KProperty<*>): MutableLiveData<T> {
        return liveData
    }
}