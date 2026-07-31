package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.lczerniawski.bettercomments.models.CustomTag

class BetterCommentsInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        val globalSettings = BetterCommentsGlobalSettings.getInstance()

        if (!globalSettings.isInitialized) {
            globalSettings.tags.addAll(DefaultTags.values.map {
                CustomTag(
                    type = it.type,
                    color = it.color,
                    backgroundColor = it.backgroundColor,
                    hasStrikethrough = it.hasStrikethrough,
                    hasUnderline = it.hasUnderline,
                    isBold = it.isBold,
                    isItalic = it.isItalic
                )
            })
            globalSettings.isInitialized = true
        }
    }
}