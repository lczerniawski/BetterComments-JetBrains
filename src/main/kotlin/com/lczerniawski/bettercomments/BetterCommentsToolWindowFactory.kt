package com.lczerniawski.bettercomments

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Component
import java.util.concurrent.Executors
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class BetterCommentsToolWindowFactory: ToolWindowFactory {
    private val executor = Executors.newSingleThreadExecutor()
    private val panel = JPanel()
    private val rootNode = DefaultMutableTreeNode("Founded files with comments") // TODO Name it same as in TODO built in app
    private val treeModel = DefaultTreeModel(rootNode)
    private val commentTree = Tree(treeModel)

    init {
        panel.layout = BorderLayout()
        panel.add(JBScrollPane(commentTree), BorderLayout.CENTER)
        commentTree.cellRenderer = CommentTreeCellRenderer()
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val refreshAction = object : AnAction(AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                scanForComments(project)
            }
        }

        val actionGroup = DefaultActionGroup()
        actionGroup.add(refreshAction)
        val actionToolbar = ActionManager.getInstance().createActionToolbar("Better Comments", actionGroup, true)
        actionToolbar.targetComponent = toolWindow.component;
        toolWindow.setTitleActions(actionGroup.getChildren(null).toList())
        toolWindow.contentManager.addContent(toolWindow.contentManager.factory.createContent(panel, "", false))

        commentTree.addTreeSelectionListener { event ->
            val node = event.path.lastPathComponent as DefaultMutableTreeNode
            val userObject = node.userObject
            if(userObject is CommentData) {
                val fileNode = node.parent as DefaultMutableTreeNode
                val fileData = fileNode.userObject as FileNodeData
                openFileInEditor(project, fileData.file, userObject.lineNumber, userObject.cursorPosition)
            }
        }

        scanForComments(project)
    }

    private fun openFileInEditor(project: Project, file: VirtualFile, lineNumber: Int, cursorPosition: Int) {
        FileEditorManager.getInstance(project).openTextEditor(
            OpenFileDescriptor(project, file, lineNumber - 1, cursorPosition, false), false
        )
    }

    private fun scanForComments(project: Project) {
        executor.submit {
            val baseDir = VirtualFileManager.getInstance().findFileByUrl("file://${project.basePath}")
            val fileCommentsMap = mutableMapOf<VirtualFile, List<CommentData>>()
            baseDir?.let { scanForComments(project, it, fileCommentsMap) }
            // TODO Do magic with comments, so extract correct ones and group them in files and move to them when clicked
            ApplicationManager.getApplication().invokeLater {
                updateTreeModel(fileCommentsMap)
            }
        }
    }

    private fun scanForComments(project: Project, directory: VirtualFile, fileCommentsMap: MutableMap<VirtualFile, List<CommentData>>) {
        val changeListManager = ChangeListManager.getInstance(project)

        if (directory.name == ".git") {
            return
        }

        directory.children.forEach { child ->
            if (child.isDirectory) {
                scanForComments(project, child, fileCommentsMap)
            } else {
                ApplicationManager.getApplication().runReadAction {
                    val document = FileDocumentManager.getInstance().getDocument(child)
                    document?.let {
                        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return@runReadAction

                        if (changeListManager.isIgnoredFile(virtualFile)) {
                            return@runReadAction
                        }

                        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return@runReadAction
                        val psiComments = PsiTreeUtil.collectElementsOfType(psiFile, PsiComment::class.java)
                        val comments = psiComments.map { comment ->
                            val lineNumber = document.getLineNumber(comment.textOffset) + 1
                            val cursorPosition = comment.textOffset - document.getLineStartOffset(lineNumber - 1)
                            CommentData(comment.text, lineNumber, cursorPosition)
                        }
                        if(comments.isNotEmpty()) {
                            fileCommentsMap[child] = comments
                        }
                    }
                }
            }
        }
    }

    private fun updateTreeModel(fileCommentsMap: Map<VirtualFile, List<CommentData>>) {
        rootNode.removeAllChildren()
        fileCommentsMap.forEach { (file, comments) ->
            val fileNode = DefaultMutableTreeNode(FileNodeData(file, comments))
            comments.forEach { comment ->
                fileNode.add(DefaultMutableTreeNode(comment))
            }

            rootNode.add(fileNode)
        }
        treeModel.reload()
    }

    data class FileNodeData(val file: VirtualFile, val comments: List<CommentData>)
    data class CommentData(val text: String, val lineNumber: Int, val cursorPosition: Int)
    class CommentTreeCellRenderer : DefaultTreeCellRenderer() {
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
                component.text = userObject.file.name
                component.toolTipText = "${userObject.file.name} (${userObject.comments.size})"
                component.foreground = if (selected) foreground else JBColor.GRAY
            } else if (userObject is CommentData) {
                component.icon = AllIcons.FileTypes.Text
                component.text = "(${userObject.lineNumber},${userObject.cursorPosition}) ${userObject.text}"
            }

            return component
        }
    }
}