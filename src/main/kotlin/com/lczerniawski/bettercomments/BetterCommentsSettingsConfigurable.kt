package com.lczerniawski.bettercomments

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.lczerniawski.bettercomments.components.ColorPickerEditor
import com.lczerniawski.bettercomments.components.ColorPickerRenderer
import com.lczerniawski.bettercomments.components.RemoveButtonRenderer
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
    private lateinit var tableModel: DefaultTableModel

    override fun createComponent(): JComponent? {
        settingsPanel = JPanel(BorderLayout())

        val columnNames = arrayOf("Type", "Color", "Background Color", "Strikethrough", "Underline", "Bold", "Italic", "")

        tableModel = object : DefaultTableModel(columnNames, 0) {
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

        val table = JBTable(tableModel)
        table.preferredScrollableViewportSize = Dimension(600, 400)
        table.fillsViewportHeight = true

        configureColorAndBackgroundColor(table)
        configureRemoveButton(table)

        val scrollPane = JBScrollPane(table)
        settingsPanel?.add(scrollPane, BorderLayout.CENTER)

        configureAddTagButton()

        return settingsPanel
    }

    override fun isModified(): Boolean {
        val settings = BetterCommentsSettings.instance
        if (tableModel.rowCount != settings.tags.size) {
            return true
        }

        for (i in 0 until tableModel.rowCount) {
            val tag = settings.tags[i]
            if (
                tableModel.getValueAt(i, 0) != tag.type ||
                tableModel.getValueAt(i, 1) != tag.color ||
                tableModel.getValueAt(i, 2) != (tag.backgroundColor ?: "") ||
                tableModel.getValueAt(i, 3) != tag.hasStrikethrough ||
                tableModel.getValueAt(i, 4) != tag.hasUnderline ||
                tableModel.getValueAt(i, 5) != tag.isBold ||
                tableModel.getValueAt(i, 6) != tag.isItalic
            ) {
                return true
            }
        }
        return false
    }

    override fun apply() {
        val settings = BetterCommentsSettings.instance
        val newTags = mutableListOf<CustomTag>()
        for (i in 0 until tableModel.rowCount) {
            val type = tableModel.getValueAt(i, 0) as String
            val color = tableModel.getValueAt(i, 1) as String
            val backgroundColor = tableModel.getValueAt(i, 2) as String
            val hasStrikethrough = tableModel.getValueAt(i, 3) as Boolean
            val hasUnderline = tableModel.getValueAt(i, 4) as Boolean
            val isBold = tableModel.getValueAt(i, 5) as Boolean
            val isItalic = tableModel.getValueAt(i, 6) as Boolean

            val tag = CustomTag(
                type = type,
                color = color,
                backgroundColor = backgroundColor.ifBlank { null },
                hasStrikethrough = hasStrikethrough,
                hasUnderline = hasUnderline,
                isBold = isBold,
                isItalic = isItalic
            )
            newTags.add(tag)
        }
        settings.tags = newTags

        refreshCommentsInEditor()
    }

    override fun reset() {
        tableModel.rowCount = 0
        loadSettings()
    }

    override fun getDisplayName(): String {
        return "Better Comments"
    }

    override fun getId(): String = "com.lczerniawski.bettercomments.settings"

    private fun configureAddTagButton() {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add Tag")
        addButton.addActionListener {
            tableModel.addRow(arrayOf("", "", "", false, false, false, false, JButton("Remove")))
        }
        buttonPanel.add(addButton)
        settingsPanel?.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun configureRemoveButton(table: JBTable) {
        table.columnModel.getColumn(7).cellRenderer = RemoveButtonRenderer()
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val column = table.columnAtPoint(e.point)
                if (column == 7) {
                    tableModel.removeRow(row)
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
        val settings = BetterCommentsSettings.instance
        settings.tags.forEach { tag ->
            tableModel.addRow(
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

    private fun refreshCommentsInEditor() {
        val editors = EditorFactory.getInstance().allEditors
        for (editor in editors) {
            CommentsHighlighter.applyCustomHighlighting(editor)
        }
    }

}