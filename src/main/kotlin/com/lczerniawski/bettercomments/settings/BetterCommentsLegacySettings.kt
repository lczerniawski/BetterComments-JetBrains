package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.components.*
import com.lczerniawski.bettercomments.models.CustomTag

@State(
    name = "BetterCommentsSettings",
    storages = [Storage("BetterCommentsSettings.xml", roamingType = RoamingType.DISABLED)]
)
@Service(Service.Level.APP)
class BetterCommentsLegacySettings : PersistentStateComponent<BetterCommentsLegacySettings.State> {
    var tags: MutableList<CustomTag> = mutableListOf()
    var migrated: Boolean = false

    class State {
        var tags: MutableList<CustomTag> = mutableListOf()
        var migrated: Boolean = false
    }

    override fun getState(): State {
        val state = State()
        state.tags = tags
        state.migrated = migrated
        return state
    }

    override fun loadState(state: State) {
        tags = state.tags
        migrated = state.migrated
    }

    companion object {
        fun getInstance(): BetterCommentsLegacySettings = service()
    }
}