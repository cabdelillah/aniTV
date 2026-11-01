package app.anitv.ui

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import app.anitv.core.model.Anime
import app.anitv.core.model.PlaybackRequest

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data class Details(val anime: Anime) : Screen
    data class Player(val request: PlaybackRequest) : Screen
}

/** A minimal back stack; screens are plain data, so no navigation library is needed. */
class Navigator(start: Screen = Screen.Home) {
    private val stack = mutableStateListOf(start)

    val current: Screen get() = stack.last()
    val screens: List<Screen> get() = stack
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    /** Replaces the top screen, e.g. to move from one episode to the next. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

/**
 * Keeps one model per screen alive while that screen is on the back stack, so going back to a
 * screen shows it instantly instead of reloading. Each model gets its own scope, cancelled when
 * its screen leaves the stack.
 */
class ScreenModels(private val parent: CoroutineScope) {
    private class Entry(val model: Any, val scope: CoroutineScope)
    private val entries = HashMap<Any, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: Any, create: (CoroutineScope) -> T): T {
        entries[key]?.let { return it.model as T }
        val scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
        return create(scope).also { entries[key] = Entry(it, scope) }
    }

    /** Drops models whose key is no longer in [alive]. */
    fun retain(alive: Set<Any>) {
        val dead = entries.keys - alive
        for (key in dead) entries.remove(key)?.scope?.cancel()
    }
}
