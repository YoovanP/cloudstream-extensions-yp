package com.local.flickystream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class FlickyStreamPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(FlickyStreamProvider())
    }
}