package com.local.popcornmovies

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PopcornMoviesPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(PopcornMoviesProvider())
    }
}