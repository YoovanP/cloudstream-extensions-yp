package com.local.rive

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class RivePlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(RiveProvider())
    }
}