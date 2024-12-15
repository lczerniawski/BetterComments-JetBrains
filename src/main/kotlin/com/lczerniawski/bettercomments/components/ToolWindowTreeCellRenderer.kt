package com.lczerniawski.bettercomments.components

import com.intellij.icons.AllIcons
import com.lczerniawski.bettercomments.models.CommentNodeData
import com.lczerniawski.bettercomments.models.FileNodeData
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class ToolWindowTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus) as JLabel
        val node = value as DefaultMutableTreeNode
        val userObject = node.userObject

        if (node.isRoot) {
            component.icon = null
        } else if (userObject is FileNodeData) {
            val fileIcon = userObject.file.fileType.icon
            component.icon = fileIcon
            val itemsLabel = if (userObject.comments.size == 1) "item" else "items"
            component.text = "<html>${userObject.file.name} <span style='color:gray;'>${userObject.comments.size} $itemsLabel</span></html>"
            component.toolTipText = "${userObject.file.name} ${userObject.comments.size} $itemsLabel"
        } else if (userObject is CommentNodeData) {
            component.icon = AllIcons.FileTypes.Text
            component.text = "<html><span style='color:gray;'>${userObject.lineNumber}</span> ${userObject.text}</html>"
        }

        return component
    }
}