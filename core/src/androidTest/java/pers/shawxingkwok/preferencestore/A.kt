package pers.shawxingkwok.preferencestore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    val migration = object : DataMigration<Preferences>{
        override suspend fun shouldMigrate(currentData: Preferences): Boolean {
            MLog.d(23, s)
            return false
        }

        override suspend fun migrate(currentData: Preferences): Preferences {
            TODO("Not yet implemented")
        }
        override suspend fun cleanUp() {
            TODO("Not yet implemented")
        }
    }

    val Context.store by preferencesDataStore("a", produceMigrations = { listOf(migration) })
    val store = appContext.store
    val key = stringPreferencesKey("s")

    val flow = store.data.onStart {
        store.edit {
            it[key] = "S"
        }
    }

    val s = "Fjp"

    @Test
    fun foo() = runBlocking{
        flow.collect{
            MLog.d(it[key])
        }
    }
}