package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class LordFlixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(LordFlixProvider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(Vidsrccc())
        registerExtractorAPI(Megacloud())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(Videasy())
    }
}
