package pers.shawxingkwok.kdatastore.demo.settings

import android.content.Context
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import pers.shawxingkwok.androidutil.AppContext
import pers.shawxingkwok.kdatastore.KDataStore

object _Settings : KDataStore("settings"){
    val isVip by store<Boolean>(false)

    init {
        val name: String = TODO("preferences name")
        val sp = AppContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        if (sp.all.any()){
            isVip.value = sp.getBoolean(TODO("vip key"), false)
            AppContext.deleteSharedPreferences(name)
        }
    }
}

@Serializable
class X(var i: Int)

object Settings : KDataStore("settings"){
    val x by store(X(0))
}

fun foo() {
    // wrong
    Settings.x.value.i++

    // right
    val newI = Settings.x.value.i + 1
    Settings.x.value = X(newI)

    // or in this way
    Settings.x.update { X(it.i + 1) }
}