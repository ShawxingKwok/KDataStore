package pers.shawxingkwok.kdatastore.hidden

import android.util.Log
import pers.shawxingkwok.androidutil.KLog
import pers.shawxingkwok.kdatastore.BuildConfig

internal actual object MLog : KLog("KDS", BuildConfig.DEBUG) {
    actual override fun e(obj: Any?){
        log(Log.ERROR, obj, null, null)
    }

    actual override fun e(obj: Any?, tr: Throwable){
        log(Log.ERROR, obj, null, tr)
    }
}