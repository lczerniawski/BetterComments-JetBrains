package com.lczerniawski.bettercomments.toolwindow

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager

class ThemeChangeListener : UISettingsListener {
    private val iconProvider = BetterCommentsIconProvider()

    override fun uiSettingsChanged(uiSettings: UISettings) {
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            project?.let {
                val toolWindow = ToolWindowManager.getInstance(it).getToolWindow("Better Comments")
                toolWindow?.let { tw ->
                    val icon = iconProvider.getIcon()
                    tw.setIcon(icon)
                }
            }
        }
    }
}