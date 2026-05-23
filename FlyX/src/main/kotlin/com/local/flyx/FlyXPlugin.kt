package com.local.flyx

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class FlyXPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(FlyXProvider())
    }
}