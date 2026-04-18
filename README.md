# cloudstream-extensions-yp

> A collection of 30 custom [Cloudstream 3](https://github.com/recloudstream/cloudstream) extension providers for popular streaming aggregator and embed-frontend sites.

---

## Providers

| Extension | Site | Type |
|-----------|------|------|
| Aether | [aether.mom](https://aether.mom) | Embed Frontend |
| Anixtv | [anixtv.us.cc](https://anixtv.us.cc) | Embed Frontend |
| CineBolt | [cinebolt.net](https://cinebolt.net) | Aggregator |
| Cineby | [cineby.sc](https://www.cineby.sc) | Embed Frontend |
| Cinegram | [cinegram.net](https://cinegram.net) | Embed Frontend |
| CinemaOS | [cinemaos.live](https://cinemaos.live) | Aggregator |
| Cinetaro | [cinetaro.tv](https://cinetaro.tv) | Aggregator |
| Cinezo | [cinezo.net](https://cinezo.net) | Aggregator |
| DuloTV | [dulo.tv](https://dulo.tv) | Aggregator |
| EE3 | [ee3.me](https://ee3.me) | Single Server |
| Filmex | [filmex.to](https://filmex.to) | Aggregator |
| FilmyTime | [filmytime.site](https://filmytime.site) | Aggregator |
| FlickyStream | [flickystream.ru](https://flickystream.ru) | Embed Frontend |
| Flixer | [flixer.su](https://flixer.su) | Aggregator |
| FlyX | [tv.vynx.cc](https://tv.vynx.cc) | Aggregator |
| IceFY | [icefy.top](https://icefy.top) | Aggregator |
| LordFlix | [lordflix.org](https://lordflix.org) | Embed Frontend |
| NexVid | [nexvid.online](https://nexvid.online) | Aggregator |
| PopcornMovies | [popcornmovies.org](https://popcornmovies.org) | Embed Frontend |
| Poprink | [popr.ink](https://popr.ink) | Aggregator |
| Primeshows | [primeshows.uk](https://www.primeshows.uk) | Aggregator |
| PStream | [pstream.net](https://pstream.net) | Embed Frontend |
| Rive | [rivestream.org](https://rivestream.org) | Embed Frontend |
| SanuFlix | [sanuflix-web-v2.pages.dev](https://sanuflix-web-v2.pages.dev) | Embed Frontend |
| ShuttleTV | [shuttletv.su](https://shuttletv.su) | Embed Frontend |
| 67Movies | [67movies.net](https://67movies.net) | Embed Frontend |
| SpenFlix | [watch.spencerdevs.xyz](https://watch.spencerdevs.xyz) | Embed Frontend |
| VoidFlix | [flixzy.pages.dev](https://flixzy.pages.dev) | Embed Frontend |
| XPrime | [xprime.su](https://xprime.su) | Aggregator |
| ZetMoon | [zetmoon.live](https://zetmoon.live) | Aggregator |

---

## How it Works

All providers follow the same pattern:

1. **Search & Metadata** — Queries the [TMDB API](https://www.themoviedb.org/documentation/api) using the site's native TMDB-ID-based URL scheme
2. **Video Links** — Loads from a rotating list of public embed servers (vidlink, vidsrc, embed.su, autoembed, multiembed)
3. **Episodes** — TV seasons and episode counts are fetched directly from TMDB's `append_to_response=seasons` endpoint

Each provider is a self-contained Gradle module with its own `build.gradle.kts`, `AndroidManifest.xml`, a `@CloudstreamPlugin` registration class, and the provider implementation.

---

## Building

**Requires:** Android Studio (for the Android SDK) and JDK 11+

```bash
# Clone the repo
git clone https://github.com/YoovanP/cloudstream-extensions-yp.git
cd cloudstream-extensions-yp

# Build all extensions
./gradlew assembleRelease
```

Output `.cs3` plugin files will be in each module's `build/` directory.

To build a single extension:
```bash
./gradlew :Cineby:assembleRelease
```

---

## Installing in Cloudstream

1. Build the extension (see above), or download a pre-built `.cs3` from [Releases](https://github.com/YoovanP/cloudstream-extensions-yp/releases)
2. In Cloudstream 3, go to **Settings → Extensions → Add Extension**
3. Paste the URL to the `.cs3` file, or sideload it directly from your device

---

## Notes

- These extensions use the public TMDB API. If you hit rate limits, replace `TMDB_KEY` in the source with your own key from [themoviedb.org](https://www.themoviedb.org/settings/api)
- Embed server availability changes — update the `EMBED_MOVIE` / `EMBED_TV` lists in each provider as needed
- Some sites (e.g. PopcornMovies) are behind Cloudflare and may require a bypass interceptor to work in production
