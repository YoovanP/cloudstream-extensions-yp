# Site Extension Lab

This pack contains one Cloudstream extension module per requested site name.

Each module includes:
- `build.gradle.kts` with cloudstream metadata
- Plugin entry class annotated with `CloudstreamPlugin`
- A thin provider class that uses the shared `TmdbCatalogProvider`
- Shared implementations for TMDB catalog browsing/search/details and Videasy stream/subtitle resolution

Total modules: 32

## How It Connects To CloudStream

CloudStream loads extension artifacts, not source folders.

1. `./gradlew make makePluginsJson` builds each module into a `.cs3` file and creates `build/plugins.json`.
2. `plugins.json` describes each `.cs3` file with name, version, status, language, and URL.
3. The app downloads or locally loads a `.cs3`, reads its generated `manifest.json`, then instantiates the class annotated with `CloudstreamPlugin`.
4. Each plugin calls `registerMainAPI(...)`, which adds the provider to CloudStream's provider list.

For local testing, copy generated `.cs3` files into the app's local plugins folder. For repo installation, publish `build/plugins.json`, the `.cs3` files, and a repository manifest that points to that plugin list.

Once pushed to GitHub, add this repository manifest in CloudStream:

```text
https://raw.githubusercontent.com/YoovanP/cloudstream-extensions-yp/main/repo.json
```

If the source branch is `master`, use `/master/repo.json` instead of `/main/repo.json`.

## Build

This folder now includes a Gradle wrapper.

Create `local.properties` with your Android SDK path. A template is included at `local.properties.example`:

```properties
sdk.dir=C:\\tmp\\android-sdk
```

Then run:

```powershell
.\gradlew.bat make makePluginsJson
```

The fallback repository is set to `https://github.com/YoovanP/cloudstream-extensions-yp`.
The repo also includes `gradle.properties` so the 32-module pack builds serially without exhausting the Kotlin daemon heap.

## Streaming Sites
- Cineby -> module `Cineby` -> https://www.cineby.sc/
- XPrime -> module `XPrime` -> https://xprime.su/
- Rive -> module `Rive` -> https://rivestream.org/
- LordFlix -> module `LordFlix` -> https://lordflix.org/
- PopcornMovies -> module `PopcornMovies` -> https://popcornmovies.org/
- 67Movies -> module `M67Movies` -> https://67movies.net/
- FlickyStream -> module `FlickyStream` -> https://flickystream.ru/
- Aether -> module `Aether` -> https://aether.mom/
- Cinegram -> module `Cinegram` -> https://cinegram.net/
- ShuttleTV -> module `ShuttleTV` -> https://shuttletv.su/
- SpenFlix -> module `SpenFlix` -> https://watch.spencerdevs.xyz/
- Cinetaro -> module `Cinetaro` -> https://cinetaro.tv/

## Single Server
- NEPU -> module `NEPU` -> https://nepu.to/
- EE3 -> module `EE3` -> https://ee3.me/
- yFlix -> module `YFlix` -> https://yflix.to/

## Stream Aggregators
- Flixer -> module `Flixer` -> https://flixer.su
- Cinezo -> module `Cinezo` -> https://www.cinezo.net/
- FlyX -> module `FlyX` -> https://tv.vynx.cc/
- Filmex -> module `Filmex` -> https://filmex.to/
- CinemaOS -> module `CinemaOS` -> https://cinemaos.live/
- Poprink -> module `Poprink` -> https://www.popr.ink/
- FilmyTime -> module `FilmyTime` -> https://www.filmytime.site/
- CineBolt -> module `CineBolt` -> https://cinebolt.net/
- Primeshows -> module `PrimeShows` -> https://www.primeshows.uk/
- NexVid -> module `NexVid` -> https://nexvid.online/
- IceFY -> module `IceFY` -> https://icefy.top/
- P-Stream (Fork) -> module `PStreamFork` -> https://pstream.net/
- SanuFlix -> module `SanuFlix` -> https://sanuflix-web-v2.pages.dev/
- dulo.tv -> module `DuloTv` -> https://dulo.tv/
- ZetMoon -> module `ZetMoon` -> https://zetmoon.live/

## Embed Frontends
- Anixtv -> module `Anixtv` -> https://anixtv.us.cc/
- VoidFlix -> module `VoidFlix` -> https://flixzy.pages.dev/

## Provider Pattern

Each module keeps a tiny site-specific provider:

```kotlin
class CinebyProvider : TmdbCatalogProvider(
    siteTitle = "Cineby",
    siteUrl = "https://www.cineby.sc/"
)
```

The shared provider handles the app-facing behavior CloudStream needs:
- TMDB-powered home rows, search results, movie details, and TV episode lists
- JSON payloads passed from `load(...)` into `loadLinks(...)`
- Videasy source lookup/decryption into playable HLS/MP4 links
- subtitle emission from the same source bundle

Some original websites use private tokens, browser extensions, or blocked JavaScript APIs. For those, this shared runtime favors reliable CloudStream playback over brittle HTML scraping of a SPA shell.
