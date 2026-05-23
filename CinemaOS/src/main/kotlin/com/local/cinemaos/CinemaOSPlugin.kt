package com.local.cinemaos

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CinemaOSPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(CinemaOSProvider())
    }
}