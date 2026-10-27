package com.lczerniawski.bettercomments.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import com.lczerniawski.bettercomments.models.CommentNodeData
import com.lczerniawski.bettercomments.models.CustomTag
import com.lczerniawski.bettercomments.models.FileNodeData
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

@Suppress("DEPRECATION") // Direct registration and cleanup are needed for these test windows.
class BetterCommentsNavigationTest : BasePlatformTestCase() {
    fun testFactoryKeepsEachToolWindowContentIndependent() {
        val manager = ToolWindowManager.getInstance(project)
        val first = manager.registerToolWindow("First comments window") {}
        val second = manager.registerToolWindow("Second comments window") {}
        try {
            val factory = BetterCommentsToolWindowFactory()
            factory.init(first)
            factory.createToolWindowContent(project, first)
            factory.init(second)
            factory.createToolWindowContent(project, second)

            val firstTree = UIUtil.findComponentOfType(first.contentManager.contents.single().component, Tree::class.java)
            val secondTree = UIUtil.findComponentOfType(second.contentManager.contents.single().component, Tree::class.java)
            assertNotNull("Opening another tool window must not remove the first window's results", firstTree)
            assertNotNull(secondTree)
            assertNotSame(firstTree, secondTree)
        } finally {
            manager.unregisterToolWindow(first.id)
            manager.unregisterToolWindow(second.id)
        }
    }

    fun testSelectingCommentOpensFileAtItsPosition() {
        val manager = ToolWindowManager.getInstance(project)
        val window = manager.registerToolWindow("Comment navigation test") {}
        try {
            val factory = BetterCommentsToolWindowFactory()
            factory.init(window)
            factory.createToolWindowContent(project, window)
            val tree = UIUtil.findComponentOfType(window.contentManager.contents.single().component, Tree::class.java)!!
            val file = myFixture.addFileToProject("comments.txt", "header\n// TODO first\n// TODO second\n").virtualFile
            val comments = listOf(
                CommentNodeData("first", 2, 3, CustomTag(color = "#ff0000")),
                CommentNodeData("second", 3, 3, CustomTag(color = "#ff0000"))
            )
            val root = DefaultMutableTreeNode("root")
            val fileNode = DefaultMutableTreeNode(FileNodeData(file, comments))
            root.add(fileNode)
            comments.forEach { fileNode.add(DefaultMutableTreeNode(it)) }
            tree.model = DefaultTreeModel(root)

            tree.selectionPath = TreePath(fileNode.path)
            comments.forEachIndexed { index, comment ->
                val node = fileNode.getChildAt(index) as DefaultMutableTreeNode
                tree.selectionPath = TreePath(node.path)
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                assertNotNull("Selecting a comment should open its file", editor)
                assertEquals(file, FileEditorManager.getInstance(project).selectedFiles.single())
                assertEquals(comment.lineNumber - 1, editor!!.caretModel.logicalPosition.line)
                assertEquals(comment.cursorPosition, editor.caretModel.logicalPosition.column)
            }
            val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
            editor.caretModel.moveToOffset(0)
            tree.clearSelection()
            assertEquals("Clearing selection must not navigate to the old comment", 0, editor.caretModel.offset)
        } finally {
            manager.unregisterToolWindow(window.id)
        }
    }
}
