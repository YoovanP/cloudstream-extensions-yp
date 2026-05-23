package com.local.xprime

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class XPrimePlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(XPrimeProvider())
    }
}