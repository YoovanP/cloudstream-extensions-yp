package com.local.nepu

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class NEPUPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(NEPUProvider())
    }
}