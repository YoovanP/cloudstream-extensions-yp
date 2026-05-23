package com.local.icefy

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class IceFYPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(IceFYProvider())
    }
}