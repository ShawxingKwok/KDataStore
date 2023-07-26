package pers.shawxingkwok.kdatastore.hidden

internal expect object MLog {
    fun e(obj: Any?)
    fun e(obj: Any?, tr: Throwable)
}