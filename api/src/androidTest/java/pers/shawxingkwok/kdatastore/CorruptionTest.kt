package pers.shawxingkwok.kdatastore

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorruptionTest  {
    val ds = PreferenceDataStoreFactory.create {
        MyInitializer.context.preferencesDataStoreFile("corruptionTest")
    }

    @Test
    fun start(){
        runBlocking {
            ds.data.first()
            ds.edit {
                it[stringPreferencesKey("Fd")] = "gfrigk"
            }
        }
    }
}