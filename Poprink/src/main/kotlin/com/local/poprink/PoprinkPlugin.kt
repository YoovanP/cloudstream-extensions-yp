package com.local.poprink

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PoprinkPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(PoprinkProvider())
    }
}