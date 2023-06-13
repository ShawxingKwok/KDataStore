package pers.shawxingkwok.kdatastore

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class Destroy {

    @Test
    fun destroy(){
        val file = context.preferencesDataStoreFile("settings_backup")
        val bytes = file.readBytes().take(100)
        file.writeBytes(bytes.toByteArray())
        MLog("destroyed", file.readBytes().size)
    }
}