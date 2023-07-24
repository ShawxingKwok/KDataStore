package pers.shawxingkwok.kdatastore.demo.settings

import android.content.Context
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