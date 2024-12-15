package com.lczerniawski.bettercomments

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.lczerniawski.bettercomments.models.CustomTag

class BetterCommentsInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = BetterCommentsSettings.instance
        if (!settings.isInitialized) {
            settings.tags.addAll(DefaultTags.values.map {
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
            settings.isInitialized = true
        }
    }
}