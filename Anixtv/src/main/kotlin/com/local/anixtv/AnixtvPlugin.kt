package com.local.anixtv

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class AnixtvPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(AnixtvProvider())
    }
}