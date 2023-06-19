package pers.shawxingkwok.kdatastore.viewjava.settings

import android.content.Context
import androidx.startup.Initializer
import kotlin.concurrent.thread

internal class MyInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        thread { Settings }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}