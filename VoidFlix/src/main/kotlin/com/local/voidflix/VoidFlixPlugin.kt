package com.local.voidflix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class VoidFlixPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(VoidFlixProvider())
    }
}