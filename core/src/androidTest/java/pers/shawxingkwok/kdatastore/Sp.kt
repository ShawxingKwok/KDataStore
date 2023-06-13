package pers.shawxingkwok.kdatastore

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Sp : SharedPreferences by context.getSharedPreferences("gni", Context.MODE_PRIVATE){

    @Test
    fun start(){
        edit(commit = true) {
            putBoolean("Fd", true)
        }
        getBoolean("Fd", false).let(::println)
    }
}