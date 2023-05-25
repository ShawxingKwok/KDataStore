package pers.apollokwok.preferencestore.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import pers.apollokwok.preferencestore.PreferenceStore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
public fun <T: Any> PreferenceStore.Flow<T>.collectAsDefaultState(
    context: CoroutineContext = EmptyCoroutineContext
)
: State<T> =
    collectAsState(default, context)