package com.local.nexvid

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class NexVidPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(NexVidProvider())
    }
}