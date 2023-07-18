package pers.shawxingkwok.kdatastore

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import pers.shawxingkwok.androidutil.AppContext

@RunWith(AndroidJUnit4::class)
class A {
    @Test
    fun start(){
        Log.d("KLOG", AppContext.toString())
    }
}