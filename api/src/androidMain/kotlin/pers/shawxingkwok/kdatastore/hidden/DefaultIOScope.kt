package pers.shawxingkwok.kdatastore.hidden

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal actual val DefaultIOScope: CoroutineScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.IO)
}