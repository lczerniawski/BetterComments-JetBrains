package com.lczerniawski.bettercomments

import com.intellij.openapi.components.*
import com.lczerniawski.bettercomments.models.CustomTag

@State(name = "BetterCommentsSettings", storages = [Storage("BetterCommentsSettings.xml")])
@Service
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
        val instance: BetterCommentsSettings
            get() = service()
    }
}