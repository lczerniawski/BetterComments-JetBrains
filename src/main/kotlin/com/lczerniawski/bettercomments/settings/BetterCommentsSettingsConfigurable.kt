package com.lczerniawski.bettercomments.settings

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
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

private const val TYPE_COLUMN = 0
private const val COLOR_COLUMN = 1
private const val BACKGROUND_COLOR_COLUMN = 2
private const val STRIKETHROUGH_COLUMN = 3
private const val UNDERLINE_COLUMN = 4
private const val BOLD_COLUMN = 5
private const val ITALIC_COLUMN = 6
private const val CONTINUATIONS_COLUMN = 7
private const val REMOVE_COLUMN = 8

class BetterCommentsSettingsConfigurable : SearchableConfigurable {
    private var settingsPanel: JPanel? = null
    private var mainPanel: JPanel? = null
    private lateinit var globalTableModel: DefaultTableModel
    private lateinit var projectTableModel: DefaultTableModel
    private val project: Project
        get() = ProjectManager.getInstance().openProjects.firstOrNull() ?: throw IllegalStateException("No open projects")

    override fun createComponent(): JComponent? {
        settingsPanel = JPanel(BorderLayout())
        globalTableModel = createTagTableModel()
        projectTableModel = createTagTableModel()

        loadSettings()

        mainPanel = JPanel(java.awt.GridLayout(2, 1))
        settingsPanel?.add(mainPanel, BorderLayout.CENTER)

        val globalPanel = JPanel(BorderLayout())
        globalPanel.border = javax.swing.border.TitledBorder("Global Settings")
        globalPanel.add(JBScrollPane(createTagTable(globalTableModel)), BorderLayout.CENTER)
        configureAddTagButton(globalTableModel, globalPanel)
        mainPanel?.add(globalPanel)

        // Local Table
        mainPanel?.add(createProjectSettingsPanel())

        return settingsPanel
    }

    private fun createTagTable(model: DefaultTableModel): JBTable {
        val table = JBTable(model)
        table.preferredScrollableViewportSize = Dimension(600, 200)
        table.fillsViewportHeight = true
        table.columnModel.getColumn(CONTINUATIONS_COLUMN).preferredWidth = JBUI.scale(105)
        configureColorAndBackgroundColor(table)
        configureRemoveButton(table, model)
        return table
    }

    private fun createTagTableModel(): DefaultTableModel {
        val columnNames = arrayOf("Type", "Color", "Background Color", "Strikethrough", "Underline", "Bold", "Italic", "Continuations", "")
        return object : DefaultTableModel(columnNames, 0) {
            override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
                STRIKETHROUGH_COLUMN, UNDERLINE_COLUMN, BOLD_COLUMN, ITALIC_COLUMN, CONTINUATIONS_COLUMN -> java.lang.Boolean::class.java
                REMOVE_COLUMN -> JButton::class.java
                else -> String::class.java
            }

            override fun isCellEditable(row: Int, column: Int): Boolean = column != REMOVE_COLUMN
        }
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
            val type = model.getValueAt(i, TYPE_COLUMN) as String
            val color = model.getValueAt(i, COLOR_COLUMN) as String
            val backgroundColor = model.getValueAt(i, BACKGROUND_COLOR_COLUMN) as String
            val hasStrikethrough = model.getValueAt(i, STRIKETHROUGH_COLUMN) as Boolean
            val hasUnderline = model.getValueAt(i, UNDERLINE_COLUMN) as Boolean
            val isBold = model.getValueAt(i, BOLD_COLUMN) as Boolean
            val isItalic = model.getValueAt(i, ITALIC_COLUMN) as Boolean

            tags.add(CustomTag(
                type = type,
                color = color,
                backgroundColor = backgroundColor.ifBlank { null },
                hasStrikethrough = hasStrikethrough,
                hasUnderline = hasUnderline,
                isBold = isBold,
                isItalic = isItalic,
                highlightContinuations = model.getValueAt(i, CONTINUATIONS_COLUMN) as Boolean
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
                model.getValueAt(i, TYPE_COLUMN) != tag.type ||
                model.getValueAt(i, COLOR_COLUMN) != tag.color ||
                model.getValueAt(i, BACKGROUND_COLOR_COLUMN) != (tag.backgroundColor ?: "") ||
                model.getValueAt(i, STRIKETHROUGH_COLUMN) != tag.hasStrikethrough ||
                model.getValueAt(i, UNDERLINE_COLUMN) != tag.hasUnderline ||
                model.getValueAt(i, BOLD_COLUMN) != tag.isBold ||
                model.getValueAt(i, ITALIC_COLUMN) != tag.isItalic ||
                model.getValueAt(i, CONTINUATIONS_COLUMN) != tag.highlightContinuations
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
            addTagRow(model, CustomTag())
        }
        buttonPanel.add(addButton)
        parent.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun configureRemoveButton(table: JBTable, model: DefaultTableModel) {
        table.columnModel.getColumn(REMOVE_COLUMN).cellRenderer = RemoveButtonRenderer()
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val column = table.convertColumnIndexToModel(table.columnAtPoint(e.point))
                if (column == REMOVE_COLUMN && row != -1) {
                    model.removeRow(row)
                }
            }
        })
    }

    private fun configureColorAndBackgroundColor(table: JBTable) {
        val colorEditor = ColorPickerEditor()
        table.columnModel.getColumn(COLOR_COLUMN).cellEditor = colorEditor
        table.columnModel.getColumn(BACKGROUND_COLOR_COLUMN).cellEditor = colorEditor

        val colorRenderer = ColorPickerRenderer()
        table.columnModel.getColumn(COLOR_COLUMN).cellRenderer = colorRenderer
        table.columnModel.getColumn(BACKGROUND_COLOR_COLUMN).cellRenderer = colorRenderer
    }

    private fun loadSettings() {
        globalTableModel.rowCount = 0
        projectTableModel.rowCount = 0
        BetterCommentsGlobalSettings.getInstance().tags.forEach { addTagRow(globalTableModel, it) }
        BetterCommentsProjectSettings.getInstance(project).tags.forEach { addTagRow(projectTableModel, it) }
    }

    private fun addTagRow(model: DefaultTableModel, tag: CustomTag) {
        model.addRow(
            arrayOf(
                tag.type,
                tag.color,
                tag.backgroundColor ?: "",
                tag.hasStrikethrough,
                tag.hasUnderline,
                tag.isBold,
                tag.isItalic,
                tag.highlightContinuations,
                JButton("Remove")
            )
        )
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
            projectPanel.add(JBScrollPane(createTagTable(projectTableModel)), BorderLayout.CENTER)
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