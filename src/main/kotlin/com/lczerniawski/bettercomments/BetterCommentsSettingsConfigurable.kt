package com.lczerniawski.bettercomments

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.table.TableCellEditor

class BetterCommentsSettingsConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private lateinit var tableModel: DefaultTableModel

    override fun createComponent(): JComponent? {
        settingsPanel = JPanel(BorderLayout())

        val columnNames = arrayOf("Type", "Color", "Background Color", "Strikethrough", "Underline", "Bold", "Italic", "Remove?")

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

        val colorEditor = ColorEditor()
        table.columnModel.getColumn(1).cellEditor = colorEditor
        table.columnModel.getColumn(2).cellEditor = colorEditor

        val colorRenderer = ColorRenderer()
        table.columnModel.getColumn(1).cellRenderer = colorRenderer
        table.columnModel.getColumn(2).cellRenderer = colorRenderer

        table.columnModel.getColumn(7).cellRenderer = ButtonRenderer()
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val column = table.columnAtPoint(e.point)
                if (column == 7) {
                    tableModel.removeRow(row)
                }
            }
        })

        val scrollPane = JBScrollPane(table)
        settingsPanel?.add(scrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add Tag")
        addButton.addActionListener {
            tableModel.addRow(arrayOf("", "", "", false, false, false, false, JButton("Yes")))
        }
        buttonPanel.add(addButton)
        settingsPanel?.add(buttonPanel, BorderLayout.SOUTH)

        return settingsPanel
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
                    JButton("Yes")
                )
            )
        }
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
    }

    override fun reset() {
        tableModel.rowCount = 0
        loadSettings()
    }

    override fun getDisplayName(): String {
        return "Better Comments"
    }

    inner class ColorEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
        private var currentColor: Color? = null
        private val button = JButton()
        private var hexColor: String = "#000000"

        init {
            button.isBorderPainted = false
            button.addActionListener(this)
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            hexColor = value as? String ?: "#000000"
            currentColor = try {
                Color.decode(hexColor)
            } catch (e: Exception) {
                JBColor.BLACK
            }
            button.background = currentColor
            return button
        }

        override fun getCellEditorValue(): Any {
            return hexColor
        }

        override fun actionPerformed(e: ActionEvent?) {
            val chooser = JColorChooser(currentColor)
            val dialog = JColorChooser.createDialog(button, "Pick a Color", true, chooser, {
                currentColor = chooser.color
                hexColor = String.format("#%06X", currentColor?.rgb?.and(0xFFFFFF) ?: 0)
            }, null)
            dialog.isVisible = true
            fireEditingStopped()
        }
    }

    inner class ButtonRenderer : JButton(), TableCellRenderer {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            text = "Remove"
            return this
        }
    }

    inner class ColorRenderer : JLabel(), TableCellRenderer {
        init {
            horizontalAlignment = CENTER
            verticalAlignment = CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val colorHex = value as? String ?: "#FFFFFF"
            try {
                background = Color.decode(colorHex)
            } catch (e: Exception) {
                background = JBColor.WHITE
            }
            text = colorHex
            return this
        }
    }
}