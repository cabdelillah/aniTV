package app.anitv.ui

import androidx.compose.runtime.Composable
import app.anitv.core.AniTvCore
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

/**
 * Sets up Coil once per process with the app's own HTTP client, rather than relying on
 * ServiceLoader discovery (which code shrinking on Android can break).
 */
@Composable
fun ProvideImageLoader(core: AniTvCore) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(core.http)) }
            .crossfade(true)
            .build()
    }
}
