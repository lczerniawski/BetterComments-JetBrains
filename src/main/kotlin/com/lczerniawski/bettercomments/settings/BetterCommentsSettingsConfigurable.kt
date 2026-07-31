package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.lczerniawski.bettercomments.codehiglighter.CommentsHighlighter
import com.lczerniawski.bettercomments.components.ColorPickerEditor
import com.lczerniawski.bettercomments.components.ColorPickerRenderer
import com.lczerniawski.bettercomments.components.RemoveButtonRenderer
import com.lczerniawski.bettercomments.models.CustomTag
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class BetterCommentsSettingsConfigurable : SearchableConfigurable {
    private var settingsPanel: JPanel? = null
    private var mainPanel: JPanel? = null
    private lateinit var globalTableModel: DefaultTableModel
    private lateinit var projectTableModel: DefaultTableModel
    private val project: Project
        get() = ProjectManager.getInstance().openProjects.firstOrNull() ?: throw IllegalStateException("No open projects")

    override fun createComponent(): JComponent? {
        settingsPanel = JPanel(BorderLayout())
        val columnNames = arrayOf("Type", "Color", "Background Color", "Strikethrough", "Underline", "Bold", "Italic", "")

        globalTableModel = object : DefaultTableModel(columnNames, 0) {
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when (columnIndex) {
                    1, 2 -> String::class.java
                    3, 4, 5, 6 -> java.lang.Boolean::class.java
                    7 -> JButton::class.java
                    else -> String::class.java
                }
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column != 7
            }
        }
        projectTableModel = object : DefaultTableModel(columnNames, 0) {
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when (columnIndex) {
                    1, 2 -> String::class.java
                    3, 4, 5, 6 -> java.lang.Boolean::class.java
                    7 -> JButton::class.java
                    else -> String::class.java
                }
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column != 7
            }
        }

        loadSettings()

        mainPanel = JPanel(java.awt.GridLayout(2, 1))
        settingsPanel?.add(mainPanel, BorderLayout.CENTER)

        val globalPanel = JPanel(BorderLayout())
        globalPanel.border = javax.swing.border.TitledBorder("Global Settings")
        val globalTable = JBTable(globalTableModel)
        globalTable.preferredScrollableViewportSize = Dimension(600, 200)
        globalTable.fillsViewportHeight = true
        configureColorAndBackgroundColor(globalTable)
        configureRemoveButton(globalTable, globalTableModel)
        globalPanel.add(JBScrollPane(globalTable), BorderLayout.CENTER)
        configureAddTagButton(globalTableModel, globalPanel)
        mainPanel?.add(globalPanel)

        // Local Table
        mainPanel?.add(createProjectSettingsPanel())

        return settingsPanel
    }

    override fun apply() {
        val globalSettings = BetterCommentsGlobalSettings.getInstance()
        globalSettings.tags = getTagsFromModel(globalTableModel).toMutableList()

        val localSettings = BetterCommentsProjectSettings.getInstance(project)
        localSettings.tags = getTagsFromModel(projectTableModel).toMutableList()

        refreshCommentsInEditor()
    }

    private fun getTagsFromModel(model: DefaultTableModel): List<CustomTag> {
        val tags = mutableListOf<CustomTag>()
        for (i in 0 until model.rowCount) {
            val type = model.getValueAt(i, 0) as String
            val color = model.getValueAt(i, 1) as String
            val backgroundColor = model.getValueAt(i, 2) as String
            val hasStrikethrough = model.getValueAt(i, 3) as Boolean
            val hasUnderline = model.getValueAt(i, 4) as Boolean
            val isBold = model.getValueAt(i, 5) as Boolean
            val isItalic = model.getValueAt(i, 6) as Boolean

            tags.add(CustomTag(
                type = type,
                color = color,
                backgroundColor = backgroundColor.ifBlank { null },
                hasStrikethrough = hasStrikethrough,
                hasUnderline = hasUnderline,
                isBold = isBold,
                isItalic = isItalic
            ))
        }
        return tags
    }

    override fun isModified(): Boolean {
        val globalSettings = BetterCommentsGlobalSettings.getInstance()
        if (isModelModified(globalTableModel, globalSettings.tags)) return true

        val projectSettings = BetterCommentsProjectSettings.getInstance(project)
        if (isModelModified(projectTableModel, projectSettings.tags)) return true

        return false
    }

    private fun isModelModified(model: DefaultTableModel, tags: List<CustomTag>): Boolean {
        if (model.rowCount != tags.size) return true
        for (i in 0 until model.rowCount) {
            val tag = tags[i]
            if (
                model.getValueAt(i, 0) != tag.type ||
                model.getValueAt(i, 1) != tag.color ||
                model.getValueAt(i, 2) != (tag.backgroundColor ?: "") ||
                model.getValueAt(i, 3) != tag.hasStrikethrough ||
                model.getValueAt(i, 4) != tag.hasUnderline ||
                model.getValueAt(i, 5) != tag.isBold ||
                model.getValueAt(i, 6) != tag.isItalic
            ) {
                return true
            }
        }
        return false
    }

    override fun reset() {
        globalTableModel.rowCount = 0
        projectTableModel.rowCount = 0
        loadSettings()
    }

    override fun getDisplayName(): String {
        return "Better Comments"
    }

    override fun getId(): String = "com.lczerniawski.bettercomments.settings"

    private fun configureAddTagButton(model: DefaultTableModel, parent: JPanel) {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add Tag")
        addButton.addActionListener {
            model.addRow(arrayOf("", "", "", false, false, false, false, JButton("Remove")))
        }
        buttonPanel.add(addButton)
        parent.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun configureRemoveButton(table: JBTable, model: DefaultTableModel) {
        table.columnModel.getColumn(7).cellRenderer = RemoveButtonRenderer()
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val column = table.columnAtPoint(e.point)
                if (column == 7 && row != -1) {
                    model.removeRow(row)
                }
            }
        })
    }

    private fun configureColorAndBackgroundColor(table: JBTable) {
        val colorEditor = ColorPickerEditor()
        table.columnModel.getColumn(1).cellEditor = colorEditor
        table.columnModel.getColumn(2).cellEditor = colorEditor

        val colorRenderer = ColorPickerRenderer()
        table.columnModel.getColumn(1).cellRenderer = colorRenderer
        table.columnModel.getColumn(2).cellRenderer = colorRenderer
    }

    private fun loadSettings() {
        globalTableModel.rowCount = 0
        projectTableModel.rowCount = 0
        val globalSettings = BetterCommentsGlobalSettings.getInstance()
        globalSettings.tags.forEach { tag ->
            globalTableModel.addRow(
                arrayOf(
                    tag.type,
                    tag.color,
                    tag.backgroundColor ?: "",
                    tag.hasStrikethrough,
                    tag.hasUnderline,
                    tag.isBold,
                    tag.isItalic,
                    JButton("Remove")
                )
            )
        }

        val localSettings = BetterCommentsProjectSettings.getInstance(project)
        localSettings.tags.forEach { tag ->
            projectTableModel.addRow(
                arrayOf(
                    tag.type,
                    tag.color,
                    tag.backgroundColor ?: "",
                    tag.hasStrikethrough,
                    tag.hasUnderline,
                    tag.isBold,
                    tag.isItalic,
                    JButton("Remove")
                )
            )
        }
    }

    private fun createProjectSettingsPanel(): JPanel {
        val projectSettings = BetterCommentsProjectSettings.getInstance(project)
        if (!projectSettings.isInitialized) {
            val panel = JPanel(FlowLayout(FlowLayout.CENTER))
            val button = JButton("Create project file")
            button.addActionListener {
                initializeProjectSettings()
                refreshProjectPanel()
            }
            panel.add(button)
            return panel
        } else {
            val projectPanel = JPanel(BorderLayout())
            projectPanel.border = javax.swing.border.TitledBorder("Project Settings")
            val projectTable = JBTable(projectTableModel)
            projectTable.preferredScrollableViewportSize = Dimension(600, 200)
            projectTable.fillsViewportHeight = true
            configureColorAndBackgroundColor(projectTable)
            configureRemoveButton(projectTable, projectTableModel)
            projectPanel.add(JBScrollPane(projectTable), BorderLayout.CENTER)
            configureAddTagButton(projectTableModel, projectPanel)
            return projectPanel
        }
    }

    private fun initializeProjectSettings() {
        val projectSettings = BetterCommentsProjectSettings.getInstance(project)
        projectSettings.isInitialized = true
        projectSettings.tags.addAll(DefaultTags.values.map {
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
        loadSettings()
    }

    private fun refreshProjectPanel() {
        mainPanel?.remove(1)
        mainPanel?.add(createProjectSettingsPanel())
        mainPanel?.revalidate()
        mainPanel?.repaint()
    }

    private fun refreshCommentsInEditor() {
        val editors = EditorFactory.getInstance().allEditors
        for (editor in editors) {
            CommentsHighlighter.applyCustomHighlighting(editor)
        }
    }

}