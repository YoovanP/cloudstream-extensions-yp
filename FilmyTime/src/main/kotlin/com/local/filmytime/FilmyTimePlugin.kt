package com.local.filmytime

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class FilmyTimePlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(FilmyTimeProvider())
    }
}