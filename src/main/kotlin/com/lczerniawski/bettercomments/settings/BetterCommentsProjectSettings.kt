package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "BetterCommentsSettings",
    storages = [
        Storage("betterCommentsSettings.xml")
    ])
@Service(Service.Level.PROJECT)
class BetterCommentsProjectSettings : BetterCommentsSettings() {
    companion object {
        fun getInstance(project: Project): BetterCommentsProjectSettings = project.service<BetterCommentsProjectSettings>()
    }
}