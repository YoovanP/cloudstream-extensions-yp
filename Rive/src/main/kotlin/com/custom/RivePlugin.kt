package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class RivePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(RiveProvider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(Vidsrccc())
        registerExtractorAPI(Megacloud())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(Videasy())
    }
}
