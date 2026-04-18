package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class DuloTVPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DuloTVProvider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(AetherHls())
        registerExtractorAPI(Videasy())
    }
}
