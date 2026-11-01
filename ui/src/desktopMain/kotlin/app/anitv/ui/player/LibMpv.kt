package app.anitv.ui.player

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.File

/** The subset of the libmpv C API (client.h and render.h) that aniTV uses. */
@Suppress("FunctionName")
internal interface LibMpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_error_string(error: Int): String
    fun mpv_free(data: Pointer)
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_command_async(ctx: Pointer, replyUserdata: Long, args: Array<String?>): Int
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer
    fun mpv_wakeup(ctx: Pointer)

    fun mpv_render_context_create(res: PointerByReference, mpv: Pointer, params: Pointer): Int
    fun mpv_render_context_set_update_callback(ctx: Pointer, callback: UpdateCallback?, callbackCtx: Pointer?)
    fun mpv_render_context_update(ctx: Pointer): Long
    fun mpv_render_context_render(ctx: Pointer, params: Pointer): Int
    fun mpv_render_context_free(ctx: Pointer)

    fun interface UpdateCallback : Callback {
        fun invoke(data: Pointer?)
    }

    companion object {
        const val EVENT_NONE = 0
        const val EVENT_SHUTDOWN = 1
        const val EVENT_END_FILE = 7
        const val EVENT_FILE_LOADED = 8

        const val END_FILE_REASON_EOF = 0
        const val END_FILE_REASON_ERROR = 4

        const val RENDER_PARAM_INVALID = 0
        const val RENDER_PARAM_API_TYPE = 1
        const val RENDER_PARAM_SW_SIZE = 17
        const val RENDER_PARAM_SW_FORMAT = 18
        const val RENDER_PARAM_SW_STRIDE = 19
        const val RENDER_PARAM_SW_POINTER = 20

        const val RENDER_UPDATE_FRAME = 1L

        /** Where Homebrew, MacPorts and the app bundle put libmpv. */
        private val searchPaths = listOfNotNull(
            System.getProperty("compose.application.resources.dir"),
            "/opt/homebrew/lib",
            "/usr/local/lib",
            "/opt/local/lib",
        )

        /** Null when libmpv is not installed. */
        val instance: LibMpv? by lazy {
            searchPaths.filter { File(it).isDirectory }.forEach { NativeLibrary.addSearchPath("mpv", it) }
            runCatching { Native.load("mpv", LibMpv::class.java) }.getOrNull()
        }
    }
}

/** Builds a native `mpv_render_param[]`: pairs of `{int type; void *data}`, zero-terminated. */
internal class RenderParams(vararg params: Pair<Int, Pointer?>) {
    private val entrySize = 2L * Native.POINTER_SIZE
    val memory = Memory(entrySize * (params.size + 1)).apply { clear() }

    init {
        params.forEachIndexed { i, (type, data) ->
            memory.setInt(i * entrySize, type)
            memory.setPointer(i * entrySize + Native.POINTER_SIZE, data)
        }
    }
}

internal fun cString(value: String): Memory =
    Memory(value.length + 1L).apply { setString(0, value, "UTF-8") }
