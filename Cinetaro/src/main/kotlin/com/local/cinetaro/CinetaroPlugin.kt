package com.local.cinetaro

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CinetaroPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(CinetaroProvider())
    }
}