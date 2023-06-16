package pers.shawxingkwok.kdatastore

import android.annotation.SuppressLint
import android.content.Context
import androidx.startup.Initializer
import com.tencent.mmkv.MMKV

internal class MyInitializer : Initializer<Unit> {
    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun create(context: Context) {
        Companion.context = context
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}