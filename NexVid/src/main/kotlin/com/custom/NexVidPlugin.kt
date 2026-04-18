package com.custom

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class NexVidPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(NexVidProvider())
        registerExtractorAPI(Vidlink())
    }
}
