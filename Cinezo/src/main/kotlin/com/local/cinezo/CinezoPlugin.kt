package com.local.cinezo

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CinezoPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(CinezoProvider())
    }
}