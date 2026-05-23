package com.local.dulotv

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class DuloTvPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(DuloTvProvider())
    }
}