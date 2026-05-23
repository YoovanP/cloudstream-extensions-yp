package com.local.spenflix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class SpenFlixPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(SpenFlixProvider())
    }
}