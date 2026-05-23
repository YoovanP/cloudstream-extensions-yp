package com.local.yflix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class YFlixPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(YFlixProvider())
    }
}