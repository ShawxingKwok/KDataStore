@file:Suppress("unused", "UNUSED_VARIABLE")

package pers.shawxingkwok.kdatastore.demo.settings

import android.content.Context
import pers.shawxingkwok.kdatastore.KDataStore

// @RunWith(AndroidJUnit4::class)
object _Settings : KDataStore("settings"){
    val isVip by store<Boolean>(false)

    init {
        val name: String = TODO("preferences name")
        val sp = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        if (sp.all.any()){
            isVip.value = sp.getBoolean(TODO("vip key"), false)
            appContext.deleteSharedPreferences(name)
        }
    }
}