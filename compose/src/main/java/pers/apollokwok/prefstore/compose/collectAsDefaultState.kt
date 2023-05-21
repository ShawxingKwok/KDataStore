package pers.apollokwok.prefstore.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import pers.apollokwok.prefstore.EasyDSFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
public fun <T: Any> EasyDSFlow<T>.collectAsDefaultState(
    context: CoroutineContext = EmptyCoroutineContext
)
: State<T> =
    collectAsState(default, context)