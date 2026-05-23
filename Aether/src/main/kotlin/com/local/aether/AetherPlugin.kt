package com.local.aether

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class AetherPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(AetherProvider())
    }
}