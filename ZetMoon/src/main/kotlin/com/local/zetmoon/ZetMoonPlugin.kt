package com.local.zetmoon

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class ZetMoonPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(ZetMoonProvider())
    }
}