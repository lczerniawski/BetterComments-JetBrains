package com.lczerniawski.bettercomments.components

import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class ColorPickerRenderer : JLabel(), TableCellRenderer {
    init {
        horizontalAlignment = CENTER
        verticalAlignment = CENTER
        isOpaque = true
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val colorHex = value as? String ?: ""
        if (colorHex.isBlank()) {
            background = table.background
            text = "No Color"
        } else {
            background = try {
                Color.decode(colorHex)
            } catch (e: Exception) {
                JBColor.WHITE
            }
            text = colorHex
        }
        return this
    }
}
