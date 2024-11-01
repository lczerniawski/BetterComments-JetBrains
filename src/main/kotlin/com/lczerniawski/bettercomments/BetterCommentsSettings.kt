package com.lczerniawski.bettercomments

import com.intellij.openapi.components.*

@State(name = "BetterCommentsSettings", storages = [Storage("BetterCommentsSettings.xml")])
@Service
class BetterCommentsSettings : PersistentStateComponent<BetterCommentsSettings.State> {
    var tags: MutableList<CustomTag> = mutableListOf()

    class State {
        var tags: MutableList<CustomTag> = mutableListOf()
    }

    override fun getState(): State {
        val state = State()
        state.tags = tags
        return state
    }

    override fun loadState(state: State) {
        tags = state.tags
    }

    companion object {
        val instance: BetterCommentsSettings
            get() = service()
    }
}

data class CustomTag(
    var type: String = "",
    var color: String = "",
    var hasStrikethrough: Boolean = false,
    var hasUnderline: Boolean = false,
    var backgroundColor: String? = null,
    var isBold: Boolean = false,
    var isItalic: Boolean = false
)