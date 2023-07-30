package pers.shawxingkwok.kdatastore.demo.settings

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.androidutil.AppContext
import pers.shawxingkwok.androidutil.KLog
import pers.shawxingkwok.kdatastore.KDataStore

@RunWith(AndroidJUnit4::class)
class _Settings : KDataStore("settings"){
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