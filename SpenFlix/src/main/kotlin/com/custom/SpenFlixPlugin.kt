package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class SpenFlixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SpenFlixProvider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(AetherHls())
        registerExtractorAPI(Videasy())
    }
}
