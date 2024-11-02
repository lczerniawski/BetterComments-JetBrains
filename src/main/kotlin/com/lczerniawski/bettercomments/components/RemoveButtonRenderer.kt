package com.lczerniawski.bettercomments.components

import java.awt.Component
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.*

class RemoveButtonRenderer : JButton(), TableCellRenderer {
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