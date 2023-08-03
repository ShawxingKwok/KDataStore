package pers.shawxingkwok.kdatastore

import android.annotation.SuppressLint
import android.content.Context
import androidx.startup.Initializer

public class KDataStoreInitializer : Initializer<Unit> {
    internal companion object {
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
            private set
    }

    override fun create(context: Context) {
        Companion.context = context
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}