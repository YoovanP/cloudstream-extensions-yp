package com.local.pstreamfork

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PStreamForkPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(PStreamForkProvider())
    }
}