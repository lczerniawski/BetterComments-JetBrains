package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.lczerniawski.bettercomments.models.CustomTag

@State(
    name = "BetterCommentsSettings",
    storages = [
        Storage("betterCommentsSettings.xml")
    ])
@Service(Service.Level.PROJECT)
class BetterCommentsSettings : PersistentStateComponent<BetterCommentsSettings.State> {
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
        fun getInstance(project: Project): BetterCommentsSettings = project.service<BetterCommentsSettings>()
    }
}