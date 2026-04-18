package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class CineBoltPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineBoltProvider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(AetherHls())
        registerExtractorAPI(Videasy())
    }
}
