package com.local.cinegram

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CinegramPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(CinegramProvider())
    }
}