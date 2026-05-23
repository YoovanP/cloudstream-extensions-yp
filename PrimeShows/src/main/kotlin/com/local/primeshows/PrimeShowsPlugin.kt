package com.local.primeshows

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PrimeShowsPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(PrimeShowsProvider())
    }
}