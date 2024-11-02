package com.lczerniawski.bettercomments.components

import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.TableCellEditor

class ColorPickerEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
    private var currentColor: Color? = null
    private val button = JButton()
    private var hexColor: String = ""

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
        hexColor = value as? String ?: ""
        currentColor = if (hexColor.isNotBlank()) {
            try {
                Color.decode(hexColor)
            } catch (e: Exception) {
                JBColor.BLACK
            }
        } else {
            null
        }
        button.background = currentColor ?: table.background
        return button
    }

    override fun getCellEditorValue(): Any {
        return hexColor
    }

    override fun actionPerformed(e: ActionEvent?) {
        val chooser = JColorChooser(currentColor ?: JBColor.WHITE)
        val dialog = JDialog(
            SwingUtilities.getWindowAncestor(button),
            "Pick a Color",
            Dialog.ModalityType.APPLICATION_MODAL
        )
        dialog.layout = BorderLayout()
        dialog.add(chooser, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")
        val noColorButton = JButton("No Color")

        okButton.addActionListener {
            currentColor = chooser.color
            hexColor = String.format("#%06X", currentColor?.rgb?.and(0xFFFFFF) ?: 0)
            dialog.dispose()
            fireEditingStopped()
        }
        cancelButton.addActionListener {
            dialog.dispose()
            fireEditingStopped()
        }
        noColorButton.addActionListener {
            currentColor = null
            hexColor = ""
            dialog.dispose()
            fireEditingStopped()
        }

        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)
        buttonPanel.add(noColorButton)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(button)
        dialog.isVisible = true
    }
}
