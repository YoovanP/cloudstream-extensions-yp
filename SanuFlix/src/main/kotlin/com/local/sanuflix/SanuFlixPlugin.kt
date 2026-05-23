package com.local.sanuflix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class SanuFlixPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(SanuFlixProvider())
    }
}