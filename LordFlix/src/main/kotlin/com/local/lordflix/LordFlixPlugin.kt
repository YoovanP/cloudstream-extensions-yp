package com.local.lordflix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class LordFlixPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(LordFlixProvider())
    }
}