# cloudstream-extensions-yp

> A collection of 30 custom [Cloudstream 3](https://github.com/recloudstream/cloudstream) extension providers for popular streaming aggregator and embed-frontend sites.

---

## 🚀 One-Click Install

To install all extensions in this repository at once:

1.  **Click this link** (if on a device with Cloudstream installed):
    [**Add YoovanP's Extensions**](cloudstreamrepo://raw.githubusercontent.com/YoovanP/cloudstream-extensions-yp/main/repo.json)
2.  **Alternatively**, copy and paste this URL into the app under **Settings → Extensions → Add Repository**:
    ```text
    https://raw.githubusercontent.com/YoovanP/cloudstream-extensions-yp/main/repo.json
    ```

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

1. **Search & Metadata** — Queries [TMDB API](https://www.themoviedb.org/documentation/api) directly.
2. **Video Links** — Aggregates links from multiple embed servers (vidlink, vidsrc, embed.su, etc.).
3. **Architecture** — Each provider is a standalone Gradle module for maximum stability.

---

## Development & Building

**Requires:** Android Studio (SDK) and JDK 17+

```bash
# Build all extensions locally
./gradlew assembleRelease
```

Compiled extensions can be found in `[ModuleName]/build/outputs/`.

---

## License & Disclaimer

These extensions are for educational purposes and personal use only. The authors do not host any content. Use responsibly.
