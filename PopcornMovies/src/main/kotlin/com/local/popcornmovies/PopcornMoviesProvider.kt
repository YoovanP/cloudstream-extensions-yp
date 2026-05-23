package com.local.popcornmovies

import com.local.shared.TmdbCatalogProvider

class PopcornMoviesProvider : TmdbCatalogProvider(
    siteTitle = "PopcornMovies",
    siteUrl = "https://popcornmovies.org/"
)