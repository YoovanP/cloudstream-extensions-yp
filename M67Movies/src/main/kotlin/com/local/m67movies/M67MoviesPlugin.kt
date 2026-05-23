package com.local.m67movies

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class M67MoviesPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(M67MoviesProvider())
    }
}