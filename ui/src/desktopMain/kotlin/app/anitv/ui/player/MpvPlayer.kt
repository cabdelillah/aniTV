package app.anitv.ui.player

import app.anitv.core.model.PlaybackRequest
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MpvUnavailableException : Exception(
    "Video playback on macOS needs mpv. Install it with: brew install mpv",
)

/**
 * libmpv playing into memory through its software render API. Frames land directly in Skia
 * bitmaps (double buffered) that Compose draws, so no native window or GL context is needed.
 * mpv still decodes with hardware acceleration (VideoToolbox on macOS) and copies frames back.
 */
class MpvPlayer(request: PlaybackRequest, private val onEnded: () -> Unit) : PlayerController {
    private val mpv = LibMpv.instance ?: throw MpvUnavailableException()
    private val handle: Pointer = mpv.mpv_create() ?: throw IllegalStateException("mpv_create failed")
    private val renderContext: Pointer

    /** A finished frame. [generation] changes on every frame so Compose redraws. */
    class Frame(val bitmap: Bitmap, val generation: Long)

    private val _frame = MutableStateFlow<Frame?>(null)
    val frame: StateFlow<Frame?> = _frame.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @Volatile private var running = true
    private val lock = Any()
    @Volatile private var targetWidth = 1280
    @Volatile private var targetHeight = 720
    private val wake = Semaphore(0)
    private val updateCallback = LibMpv.UpdateCallback { wake.release() } // strong ref: JNA must not GC it
    private val renderThread: Thread
    private val eventThread: Thread

    init {
        option("vo", "libmpv")
        option("hwdec", "auto-copy")
        option("terminal", "no")
        option("ytdl", "no")
        option("keep-open", "no")
        option("cache", "yes")
        option("demuxer-max-bytes", "150MiB")
        option("demuxer-readahead-secs", "60")
        option("sub-auto", "no")
        if (request.startPositionMs > 0) option("start", (request.startPositionMs / 1000.0).toString())

        val headers = request.stream.headers
        headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.let { option("user-agent", it.value) }
        headers.entries.firstOrNull { it.key.equals("Referer", true) }?.let { option("referrer", it.value) }
        val others = headers.filterKeys { !it.equals("User-Agent", true) && !it.equals("Referer", true) }
        if (others.isNotEmpty()) {
            // Comma-separated list; escape commas inside values per mpv's list syntax.
            option("http-header-fields", others.entries.joinToString(",") { (k, v) -> "$k: ${v.replace(",", "\\,")}" })
        }

        check(mpv.mpv_initialize(handle) >= 0) { "mpv_initialize failed" }

        val apiType = cString("sw")
        val params = RenderParams(LibMpv.RENDER_PARAM_API_TYPE to apiType)
        val ref = PointerByReference()
        val result = mpv.mpv_render_context_create(ref, handle, params.memory)
        check(result >= 0) { "mpv render context failed: ${mpv.mpv_error_string(result)}" }
        renderContext = ref.value
        mpv.mpv_render_context_set_update_callback(renderContext, updateCallback, null)

        renderThread = thread(name = "mpv-render", isDaemon = true) { renderLoop() }
        eventThread = thread(name = "mpv-events", isDaemon = true) { eventLoop(request) }

        command("loadfile", request.stream.url, "replace")
    }

    // region PlayerController
    override val positionMs: Long get() = (doubleProperty("time-pos") * 1000).toLong()
    override val durationMs: Long get() = (doubleProperty("duration") * 1000).toLong()
    override val isPlaying: Boolean get() = stringProperty("pause") == "no"
    override fun seekTo(positionMs: Long) {
        command("seek", (positionMs / 1000.0).toString(), "absolute")
    }
    override fun togglePlay() {
        command("cycle", "pause")
    }
    // endregion

    fun seekBy(deltaMs: Long) {
        command("seek", (deltaMs / 1000.0).toString(), "relative")
    }

    /** The size the video is shown at, in pixels; frames are rendered no larger than needed. */
    fun setViewportSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        targetWidth = width
        targetHeight = height
        wake.release()
    }

    fun release() {
        synchronized(lock) {
            if (!running) return
            running = false
        }
        wake.release()
        mpv.mpv_wakeup(handle)
        renderThread.join()
        eventThread.join()
        mpv.mpv_render_context_set_update_callback(renderContext, null, null)
        mpv.mpv_render_context_free(renderContext)
        mpv.mpv_terminate_destroy(handle)
    }

    private fun renderLoop() {
        val buffers = arrayOfNulls<Bitmap>(2)
        var index = 0
        var generation = 0L
        val format = cString("bgr0")
        val size = Memory(8)
        val stride = Memory(Native.SIZE_T_SIZE.toLong())
        var lastSize = 0 to 0

        while (running) {
            wake.tryAcquire(250, TimeUnit.MILLISECONDS)
            if (!running) break
            val flags = mpv.mpv_render_context_update(renderContext)
            val (width, height) = renderSize()
            val resized = (width to height) != lastSize
            if (flags and LibMpv.RENDER_UPDATE_FRAME == 0L && !resized) continue
            if (width <= 0 || height <= 0) continue
            lastSize = width to height

            val bitmap = buffers[index]?.takeIf { it.width == width && it.height == height }
                ?: Bitmap().also {
                    buffers[index]?.close()
                    it.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE))
                    buffers[index] = it
                }
            val pixels = bitmap.peekPixels() ?: continue
            size.setInt(0, width)
            size.setInt(4, height)
            if (Native.SIZE_T_SIZE == 8) stride.setLong(0, bitmap.rowBytes.toLong()) else stride.setInt(0, bitmap.rowBytes)

            val params = RenderParams(
                LibMpv.RENDER_PARAM_SW_SIZE to size,
                LibMpv.RENDER_PARAM_SW_FORMAT to format,
                LibMpv.RENDER_PARAM_SW_STRIDE to stride,
                LibMpv.RENDER_PARAM_SW_POINTER to Pointer(pixels.addr),
            )
            if (mpv.mpv_render_context_render(renderContext, params.memory) >= 0) {
                bitmap.notifyPixelsChanged()
                _frame.value = Frame(bitmap, ++generation)
                index = 1 - index
            }
        }
    }

    /**
     * Renders at the viewport size, but never above the video's own resolution: upscaling is
     * left to the GPU when Compose draws, which keeps CPU use low on large and Retina screens.
     */
    private fun renderSize(): Pair<Int, Int> {
        val videoW = intProperty("dwidth")
        val videoH = intProperty("dheight")
        var w = targetWidth
        var h = targetHeight
        if (videoW > 0 && videoH > 0) {
            val scale = minOf(1.0, videoW.toDouble() / w, videoH.toDouble() / h)
            w = (w * scale).toInt()
            h = (h * scale).toInt()
        }
        // Keep rows 64-byte aligned for mpv's SIMD paths.
        return (w / 16 * 16) to h
    }

    private fun eventLoop(request: PlaybackRequest) {
        while (running) {
            val event = mpv.mpv_wait_event(handle, 0.5)
            when (event.getInt(0)) {
                LibMpv.EVENT_SHUTDOWN -> return
                LibMpv.EVENT_FILE_LOADED -> request.stream.subtitles.forEachIndexed { i, sub ->
                    command("sub-add", sub.url, if (i == 0) "select" else "auto", sub.language, sub.language)
                }
                LibMpv.EVENT_END_FILE -> {
                    val data = event.getPointer(16)
                    val reason = data?.getInt(0) ?: -1
                    when (reason) {
                        LibMpv.END_FILE_REASON_EOF -> if (running) onEnded()
                        LibMpv.END_FILE_REASON_ERROR -> _error.value = "Could not play this stream"
                    }
                }
            }
        }
    }

    private fun option(name: String, value: String) {
        mpv.mpv_set_option_string(handle, name, value)
    }

    private fun command(vararg args: String) = synchronized(lock) {
        if (running) mpv.mpv_command_async(handle, 0, arrayOf(*args, null))
    }

    private fun stringProperty(name: String): String? = synchronized(lock) {
        if (!running) return null
        val ptr = mpv.mpv_get_property_string(handle, name) ?: return null
        try { ptr.getString(0, "UTF-8") } finally { mpv.mpv_free(ptr) }
    }

    private fun doubleProperty(name: String) = stringProperty(name)?.toDoubleOrNull() ?: 0.0
    private fun intProperty(name: String) = stringProperty(name)?.toIntOrNull() ?: 0
}
