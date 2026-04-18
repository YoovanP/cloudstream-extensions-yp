package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnixtvPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AnixtvProvider())
        registerExtractorAPI(Vidlink())
    }
}
