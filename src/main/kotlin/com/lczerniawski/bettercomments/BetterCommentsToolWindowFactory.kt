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
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.lczerniawski.bettercomments.components.ToolWindowTreeCellRenderer
import com.lczerniawski.bettercomments.models.CommentNodeData
import com.lczerniawski.bettercomments.models.FileNodeData
import java.awt.BorderLayout
import java.util.concurrent.Executors
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BetterCommentsToolWindowFactory: ToolWindowFactory {
    private val executor = Executors.newSingleThreadExecutor()
    private val panel = JPanel()
    private val rootNode = DefaultMutableTreeNode("Found 0 comments in 0 files")
    private val treeModel = DefaultTreeModel(rootNode)
    private val commentTree = Tree(treeModel)

    init {
        panel.layout = BorderLayout()
        panel.add(JBScrollPane(commentTree), BorderLayout.CENTER)
        commentTree.cellRenderer = ToolWindowTreeCellRenderer()
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
        actionToolbar.targetComponent = toolWindow.component
        toolWindow.setTitleActions(actionGroup.getChildren(null).toList())
        toolWindow.contentManager.addContent(toolWindow.contentManager.factory.createContent(panel, "", false))

        commentTree.addTreeSelectionListener { event ->
            val node = event.path.lastPathComponent as DefaultMutableTreeNode
            val userObject = node.userObject
            if(userObject is CommentNodeData) {
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
            val fileCommentsMap = mutableMapOf<VirtualFile, List<CommentNodeData>>()
            baseDir?.let { scanForComments(project, it, fileCommentsMap) }
            // TODO Do magic with comments, so extract correct ones and group them in files and move to them when clicked
            ApplicationManager.getApplication().invokeLater {
                updateTreeModel(fileCommentsMap, project)
            }
        }
    }

    private fun scanForComments(project: Project, directory: VirtualFile, fileCommentsMap: MutableMap<VirtualFile, List<CommentNodeData>>) {
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
                            CommentNodeData(comment.text, lineNumber, cursorPosition)
                        }
                        if(comments.isNotEmpty()) {
                            fileCommentsMap[child] = comments
                        }
                    }
                }
            }
        }
    }

    private fun updateTreeModel(fileCommentsMap: Map<VirtualFile, List<CommentNodeData>>, project: Project) {
        var commentsCounter = 0
        var filesCounter = 0
        val projectBasePath = project.basePath

        rootNode.removeAllChildren()
        val folderNodes = mutableMapOf<String, DefaultMutableTreeNode>()
        val folderFileCount = mutableMapOf<String, Int>()

        fileCommentsMap.forEach { (file, comments) ->
            val fileNode = DefaultMutableTreeNode(FileNodeData(file, comments))
            filesCounter+= 1

            comments.forEach { comment ->
                fileNode.add(DefaultMutableTreeNode(comment))
                commentsCounter += 1
            }

            val folderName = file.parent.path.substringAfter(projectBasePath!!).trimStart('/')

            if (folderName == "") {
                rootNode.add(fileNode)
            } else {
                val folderNode = folderNodes.getOrPut(folderName) {
                    val node = DefaultMutableTreeNode(folderName)
                    rootNode.add(node)
                    node
                }
                folderNode.add(fileNode)
                folderFileCount[folderName] = folderFileCount.getOrDefault(folderName, 0) + 1
            }
        }

        folderNodes.forEach{ (folderName, folderNode) ->
            val fileCount = folderFileCount[folderName] ?: 0
            val fileLabel = if (fileCount == 1) "item" else "items"
            folderNode.userObject = "<html>$folderName <span style='color:gray;'>$fileCount $fileLabel</span></html>"
        }

        val commentsLabel = if (commentsCounter == 1) "comment" else "comments"
        val filesLabel = if (filesCounter == 1) "file" else "files"
        rootNode.userObject = "Found $commentsCounter $commentsLabel in $filesCounter $filesLabel"
        treeModel.reload()
    }
}