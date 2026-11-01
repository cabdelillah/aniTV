# aniTV

Anime streaming for **Android phones and tablets**, **Android TV** and **macOS**, from one Kotlin codebase.

## Layout

| Module | What it is |
|---|---|
| `core` | Kotlin Multiplatform. Data models, AniList client (metadata, search, home rows), AniSkip client (intro/outro times), the `AnimeSource` plugin interface, title matching, watch-progress store. No UI. |
| `ui` | Compose Multiplatform. State holders (`HomeModel`, `SearchModel`, `DetailsModel`, `PlayerModel`), the phone/desktop screens, and `VideoPlayer`: **Media3/ExoPlayer** on Android, **libmpv** on macOS. |
| `androidApp` | One APK for phone and TV. Picks the touch UI or the D-pad TV UI (`androidx.tv.material3`) at launch; both use the same models and player. |
| `desktopApp` | macOS app (Compose Desktop), packaged as a DMG. |

## Sources

aniTV ships no third-party streaming sources. Metadata comes from AniList; episodes and streams come from an `AnimeSource`:

```kotlin
interface AnimeSource {
    val id: String
    val name: String
    suspend fun search(query: String): List<SourceAnime>
    suspend fun episodes(anime: SourceAnime, dub: Boolean = false): List<Episode>
    suspend fun streams(episode: Episode, dub: Boolean = false): List<Stream>
}
```

`AniTvCore.episodes(anime)` searches the selected source with the anime's titles and picks the closest match (`TitleMatcher`). A `Stream` carries its URL, HTTP headers (Referer, User-Agent, …) and subtitle tracks, which both players apply.

The built-in `DemoSource` plays public test streams for any title so the whole flow (browse → details → play → resume → next episode) works out of the box. Register your own with `AniTvCore(sources = listOf(MySource()))`.

## Building

JDK 17+.

```bash
./gradlew :androidApp:assembleRelease          # APK for phone + TV (needs the Android SDK)
./gradlew :desktopApp:run -PdesktopOnly=true    # run the desktop app
./gradlew :desktopApp:packageDmg -PdesktopOnly=true   # macOS DMG (run on a Mac)
./gradlew :core:desktopTest -PdesktopOnly=true  # core unit tests
```

`-PdesktopOnly=true` skips the Android modules, so machines without the Android SDK can build the core and desktop app.

GitHub Actions builds the APK and the DMG on every push (see the run's artifacts).

### macOS playback

The desktop player uses libmpv for decoding (hardware accelerated through VideoToolbox) and renders into Compose. Install it once:

```bash
brew install mpv
```

Desktop shortcuts: Space/K play-pause, ←/→ (J/L) seek 10 s, F fullscreen, Esc back.
