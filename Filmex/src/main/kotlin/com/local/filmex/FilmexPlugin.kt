package com.local.filmex

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class FilmexPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(FilmexProvider())
    }
}