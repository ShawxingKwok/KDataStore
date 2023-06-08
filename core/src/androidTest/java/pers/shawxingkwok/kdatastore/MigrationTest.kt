package pers.shawxingkwok.kdatastore

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class MigrationTest : KDataStore(){
    init {
        /*
        sp.edit(commit = true) {
            putBoolean("isVip", false)
            putString("name", "Apollo")
        }
        */
    }

    val isVip by bool(false)
    val name by string("Jack", backup = true)

    override fun getMigration(context: Context) = object : Migration {
        val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        override suspend fun shouldMigrate(): Boolean {
            return sp.all.any()
        }

        override suspend fun migrate() {
            sp.getBoolean("isVip", false).let { isVip.emit(it) }
            sp.getString("name", "Jack")?.let { name.emit(it) }
        }

        override suspend fun cleanUp() {
            context.deleteSharedPreferences("settings")
        }
    }

    @Test
    fun foo(){
        runBlocking {
            updateAll {
                isVip.emit(true)
                name.emit("Shawxing")
            }
            assert(isVip.first())
            assert(name.first() == "Shawxing")
        }
    }
}