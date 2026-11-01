package app.anitv.ui.presentation

/** The state of something loaded asynchronously. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

internal fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
