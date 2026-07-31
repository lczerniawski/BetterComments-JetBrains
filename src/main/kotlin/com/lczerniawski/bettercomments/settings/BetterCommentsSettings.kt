package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.lczerniawski.bettercomments.models.CustomTag

open class BetterCommentsSettings : PersistentStateComponent<BetterCommentsSettings.State> {
    var tags: MutableList<CustomTag> = mutableListOf()
    var isInitialized: Boolean = false

    class State {
        var tags: MutableList<CustomTag> = mutableListOf()
        var isInitialized: Boolean = false
    }

    override fun getState(): State {
        val state = State()
        state.tags = tags
        state.isInitialized = isInitialized
        return state
    }

    override fun loadState(state: State) {
        tags = state.tags
        isInitialized = state.isInitialized
    }

    companion object {
        fun getSettings(project: Project): BetterCommentsSettings {
            val projectSettings = BetterCommentsProjectSettings.getInstance(project)
            if (projectSettings.isInitialized) {
                return projectSettings
            }

            val globalSettings = BetterCommentsGlobalSettings.getInstance()
            return globalSettings
        }
    }
}