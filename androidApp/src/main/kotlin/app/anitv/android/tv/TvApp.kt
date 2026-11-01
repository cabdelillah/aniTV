package app.anitv.android.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import app.anitv.core.AniTvCore
import app.anitv.ui.Navigator
import app.anitv.ui.Screen
import app.anitv.ui.ScreenModels
import app.anitv.ui.presentation.DetailsModel
import app.anitv.ui.presentation.HomeModel
import app.anitv.ui.presentation.SearchModel
import app.anitv.ui.screens.PlayerScreen
import app.anitv.ui.theme.AniTvColors
import app.anitv.ui.theme.AniTvTheme

/** Android TV UI: same models as phone and desktop, laid out for 10-foot viewing and D-pad focus. */
@Composable
fun TvApp(core: AniTvCore, navigator: Navigator) {
    val appScope = rememberCoroutineScope()
    val home = remember { HomeModel(core, appScope) }
    val search = remember { SearchModel(core, appScope) }
    val models = remember { ScreenModels(appScope) }
    models.retain(navigator.screens.filterIsInstance<Screen.Details>().map { it.anime.id }.toSet())

    AniTvTheme {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = AniTvColors.Accent,
                background = AniTvColors.Background,
                surface = AniTvColors.Surface,
                surfaceVariant = AniTvColors.SurfaceHigh,
                onSurfaceVariant = AniTvColors.OnSurfaceMuted,
            ),
        ) {
            when (val screen = navigator.current) {
                Screen.Home -> TvHomeScreen(
                    model = home,
                    onOpen = { navigator.push(Screen.Details(it)) },
                    onSearch = { navigator.push(Screen.Search) },
                )
                Screen.Search -> TvSearchScreen(
                    model = search,
                    onOpen = { navigator.push(Screen.Details(it)) },
                )
                is Screen.Details -> key(screen.anime.id) {
                    TvDetailsScreen(
                        model = models.get(screen.anime.id) { DetailsModel(core, screen.anime, it) },
                        onPlay = { navigator.push(Screen.Player(it)) },
                        onOpen = { navigator.push(Screen.Details(it)) },
                    )
                }
                is Screen.Player -> key(screen.request) {
                    PlayerScreen(
                        core = core,
                        request = screen.request,
                        onBack = { navigator.pop() },
                        onNext = { navigator.replace(Screen.Player(it)) },
                        showBackButton = false,
                    )
                }
            }
        }
    }
}
