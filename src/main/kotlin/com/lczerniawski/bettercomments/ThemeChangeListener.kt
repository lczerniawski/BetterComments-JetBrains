package com.lczerniawski.bettercomments

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.wm.ToolWindow

class ThemeChangeListener(private val toolWindow: ToolWindow) : UISettingsListener {
    private val iconProvider = BetterCommentsIconProvider()

    override fun uiSettingsChanged(uiSettings: UISettings) {
        val icon = iconProvider.getIcon()
        toolWindow.setIcon(icon)
    }
}