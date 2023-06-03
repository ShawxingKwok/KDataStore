package pers.shawxingkwok.kdatastore.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pers.shawxingkwok.kdatastore.KDataStore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
public fun <T: Any> KDataStore.Flow<T>.collectAsDefaultState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> =
    collectAsState(default, context)

@Composable
public fun <T: Any> KDataStore.Flow<T>.collectAsDefaultStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
): State<T> =
    collectAsStateWithLifecycle(default, lifecycleOwner, minActiveState, context)