package com.lczerniawski.bettercomments.toolwindow

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import javax.swing.Icon

class BetterCommentsIconProvider {
    fun getIcon(): Icon {
        val isDarkTheme = !JBColor.isBright()
        return if (isDarkTheme) {
            IconLoader.getIcon("/icons/toolbar_icon_dark_13x13.svg", javaClass)
        } else {
            IconLoader.getIcon("/icons/toolbar_icon_light_13x13.svg", javaClass)
        }
    }
}