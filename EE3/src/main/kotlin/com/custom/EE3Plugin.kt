package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class EE3Plugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(EE3Provider())
        registerExtractorAPI(Vidlink())
        registerExtractorAPI(Vidsrccc())
        registerExtractorAPI(Megacloud())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(Videasy())
    }
}
