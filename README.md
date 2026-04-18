# Custom Extension Workbench

This workspace contains **32 custom Cloudstream providers** for various streaming, aggregator, and embed-frontend sites.

---

## Architecture

All providers share one of two strategies:

### Strategy A — TMDB-ID-Based Embed Frontends (28 sites)
Metadata is fetched directly from TMDB API. Video links are delivered by loading standard embed server URLs (vidsrc, vidlink, embed.su, autoembed, multiembed).

**Base class:** `TmdbEmbedBase` (in `AllProviders.kt`) — subclass and provide `mainUrl`/`name`.

### Strategy B — Scraped / Custom-API Sites (4 sites)
These require page-level HTML scraping or hitting a custom REST/GraphQL API.

---

## Files

| File | Providers |
|------|-----------|
| `CinebyProvider.kt` | Cineby |
| `CinemaOSProvider.kt` | CinemaOS |
| `FlixerProvider.kt` | Flixer |
| `FilmexProvider.kt` | Filmex |
| `AetherProvider.kt` | Aether |
| `XPrimeProvider.kt` | XPrime |
| `LordFlixProvider.kt` | LordFlix |
| `RiveProvider.kt` | Rive |
| `AllProviders.kt` | SpenFlix, Cinezo, FlyX, PopcornMovies, 67Movies, FlickyStream, Cinegram, ShuttleTV, Cinetaro, Poprink, FilmyTime, CineBolt, Primeshows, NexVid, IceFY, P-Stream, SanuFlix, dulo.tv, ZetMoon, Anixtv, VoidFlix, NEPU, EE3 |

---

## Integration

1. Copy the desired `.kt` files into your Cloudstream extensions Gradle module's `src/main/kotlin/` folder.
2. Register each provider class in your `Plugin.kt`:
   ```kotlin
   override fun load(context: Context) {
       registerMainAPI(CinebyProvider())
       registerMainAPI(CinemaOSProvider())
       registerMainAPI(FlixerProvider())
       // ... etc
   }
   ```
3. Build with `./gradlew assembleRelease`.

---

## Notes

- All TMDB-based providers use the public TMDB API key embedded in the source. Replace with your own key for production.
- Sites behind Cloudflare (PopcornMovies, NEPU) may require a Cloudflare bypass interceptor.
- Embed server availability changes frequently — add/remove URLs in the `EMBED_*` lists as needed.
