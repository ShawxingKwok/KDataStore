package pers.apollokwok.kdatastore.hidden

@PublishedApi
internal expect object MLog {
    fun e(obj: Any?)
    fun e(obj: Any?, tr: Throwable)
}