package app.anitv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.anitv.core.AniTvCore
import app.anitv.ui.presentation.DetailsModel
import app.anitv.ui.presentation.HomeModel
import app.anitv.ui.presentation.SearchModel
import app.anitv.ui.screens.DetailsScreen
import app.anitv.ui.screens.HomeScreen
import app.anitv.ui.screens.PlayerScreen
import app.anitv.ui.screens.SearchScreen
import app.anitv.ui.theme.AniTvTheme

/** The phone and desktop app. Android TV has its own focus-driven UI on the same models. */
@Composable
fun AniTvApp(core: AniTvCore, navigator: Navigator) {
    val appScope = rememberCoroutineScope()
    val home = remember { HomeModel(core, appScope) }
    val search = remember { SearchModel(core, appScope) }
    val models = remember { ScreenModels(appScope) }
    models.retain(navigator.screens.filterIsInstance<Screen.Details>().map { it.anime.id }.toSet())

    ProvideImageLoader(core)
    AniTvTheme {
        when (val screen = navigator.current) {
            Screen.Home -> HomeScreen(
                model = home,
                onOpen = { navigator.push(Screen.Details(it)) },
                onSearch = { navigator.push(Screen.Search) },
            )
            Screen.Search -> SearchScreen(
                model = search,
                onOpen = { navigator.push(Screen.Details(it)) },
                onBack = { navigator.pop() },
            )
            is Screen.Details -> key(screen.anime.id) {
                DetailsScreen(
                    model = models.get(screen.anime.id) { DetailsModel(core, screen.anime, it) },
                    onPlay = { navigator.push(Screen.Player(it)) },
                    onOpen = { navigator.push(Screen.Details(it)) },
                    onBack = { navigator.pop() },
                )
            }
            is Screen.Player -> key(screen.request) {
                PlayerScreen(
                    core = core,
                    request = screen.request,
                    onBack = { navigator.pop() },
                    onNext = { navigator.replace(Screen.Player(it)) },
                )
            }
        }
    }
}
