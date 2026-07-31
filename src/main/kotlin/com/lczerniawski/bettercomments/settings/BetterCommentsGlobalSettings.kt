package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.components.*

@State(
    name = "BetterCommentsSettings",
    storages = [Storage("BetterCommentsSettings.xml", roamingType = RoamingType.DISABLED)]
)
@Service(Service.Level.APP)
class BetterCommentsGlobalSettings : BetterCommentsSettings() {
    companion object {
        fun getInstance(): BetterCommentsGlobalSettings = service()
    }
}