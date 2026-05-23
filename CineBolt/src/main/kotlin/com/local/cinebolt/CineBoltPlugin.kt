package com.local.cinebolt

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CineBoltPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(CineBoltProvider())
    }
}