package com.lczerniawski.bettercomments.components

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.lczerniawski.bettercomments.common.parseColorWithAlpha
import com.lczerniawski.bettercomments.models.CommentNodeData
import com.lczerniawski.bettercomments.models.FileNodeData
import com.lczerniawski.bettercomments.models.FolderNodeData
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class ToolWindowTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as DefaultMutableTreeNode
        val userObject = node.userObject

        if (node.isRoot) {
            icon = null
            append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        } else if (userObject is FileNodeData) {
            icon = userObject.file.fileType.icon
            val itemsLabel = if (userObject.comments.size == 1) "item" else "items"
            append(userObject.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            append(" ${userObject.comments.size} $itemsLabel", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } else if (userObject is CommentNodeData) {
            icon = AllIcons.FileTypes.Text
            var style = SimpleTextAttributes.STYLE_PLAIN
            if (userObject.tag.isBold) {
                style = SimpleTextAttributes.STYLE_BOLD
            }

            if (userObject.tag.isItalic) {
                style = SimpleTextAttributes.STYLE_ITALIC
            }

            if (userObject.tag.hasUnderline) {
                style = SimpleTextAttributes.STYLE_UNDERLINE
            }

            if (userObject.tag.hasStrikethrough) {
                style = SimpleTextAttributes.STYLE_STRIKEOUT
            }

            var attributes = SimpleTextAttributes(style, userObject.tag.color.parseColorWithAlpha())
            if (userObject.tag.backgroundColor != null) {
                attributes = SimpleTextAttributes(style, userObject.tag.backgroundColor!!.parseColorWithAlpha(), userObject.tag.color.parseColorWithAlpha())
            }

            append("${userObject.lineNumber} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            append(userObject.text, attributes)
        } else if(userObject is FolderNodeData) {
            icon = AllIcons.Nodes.Folder
            val itemsLabel = if (userObject.itemsCounter == 1) "item" else "items"
            append(userObject.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            append(" ${userObject.itemsCounter} $itemsLabel", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}