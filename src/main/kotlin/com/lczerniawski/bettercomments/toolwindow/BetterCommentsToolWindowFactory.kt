package com.lczerniawski.bettercomments.toolwindow

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
import com.intellij.openapi.ui.ComboBox
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
import com.lczerniawski.bettercomments.common.CommentsParser
import com.lczerniawski.bettercomments.components.ToolWindowTreeCellRenderer
import com.lczerniawski.bettercomments.models.CommentNodeData
import com.lczerniawski.bettercomments.models.FileNodeData
import com.lczerniawski.bettercomments.models.FolderNodeData
import com.lczerniawski.bettercomments.settings.BetterCommentsSettings
import java.awt.BorderLayout
import java.awt.CardLayout
import java.util.concurrent.Executors
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BetterCommentsToolWindowFactory: ToolWindowFactory {
    private val foldersToExclude = arrayOf(".git")
    private val searchTypes = SearchTypes.values()
    private val allCommentsType = "All"

    private val executor = Executors.newSingleThreadExecutor()
    private val panel = JPanel(CardLayout())
    private val rootNode = DefaultMutableTreeNode("Found 0 comments in 0 files")
    private val treeModel = DefaultTreeModel(rootNode)
    private val commentTree = Tree(treeModel)
    private val progressLabel = JLabel("Search in progress...", JLabel.CENTER)
    private val startupLabel = JLabel("Click the refresh button to search for comments in your project.", JLabel.CENTER)
    private val scopeTypeComboBox = ComboBox(searchTypes.map { it.description }.toTypedArray())

    private val iconProvider = BetterCommentsIconProvider()

    private lateinit var commentsParser: CommentsParser
    private lateinit var commentTypeComboBox: ComboBox<String>
    private lateinit var project: Project

    override fun init(toolWindow: ToolWindow) {
        val treePanel = JPanel(BorderLayout())
        treePanel.add(JBScrollPane(commentTree), BorderLayout.CENTER)
        commentTree.cellRenderer = ToolWindowTreeCellRenderer()
        commentTree.addTreeSelectionListener { event ->
            val node = event.path.lastPathComponent as DefaultMutableTreeNode
            val userObject = node.userObject
            if (userObject is CommentNodeData) {
                val fileNode = node.parent as DefaultMutableTreeNode
                val fileData = fileNode.userObject as FileNodeData
                openFileInEditor(project, fileData.file, userObject.lineNumber, userObject.cursorPosition)
            }
        }

        panel.add(startupLabel, "Startup")
        panel.add(treePanel, "Tree")
        panel.add(progressLabel, "Progress")

        val icon = iconProvider.getIcon()
        toolWindow.setIcon(icon)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.project = project
        this.commentsParser = CommentsParser(project)
        this.commentTypeComboBox = getComboBoxForCommentsType(project)

        val refreshAction = object : AnAction("Refresh", "Refresh the comments list", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                scanForComments()
            }
        }

        val actionGroup = DefaultActionGroup()
        actionGroup.add(refreshAction)
        val actionToolbar = ActionManager.getInstance().createActionToolbar("Better Comments", actionGroup, true)
        actionToolbar.targetComponent = toolWindow.component


        val scopePanel = JPanel()
        scopePanel.layout = BoxLayout(scopePanel, BoxLayout.X_AXIS)
        val scopeLabel = JLabel("Scope: ")
        scopePanel.add(scopeLabel)
        scopePanel.add(scopeTypeComboBox)

        val commentTypePanel = JPanel()
        commentTypePanel.layout = BoxLayout(commentTypePanel, BoxLayout.X_AXIS)
        val commentTypeLabel = JLabel("Comment Type: ")
        scopePanel.add(commentTypeLabel)
        scopePanel.add(commentTypeComboBox)

        val filtersPanel = JPanel()
        filtersPanel.layout = BoxLayout(filtersPanel, BoxLayout.X_AXIS)
        filtersPanel.add(scopePanel)

        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(filtersPanel, BorderLayout.NORTH)
        contentPanel.add(panel, BorderLayout.CENTER)

        toolWindow.setTitleActions(mutableListOf(refreshAction))
        toolWindow.contentManager.addContent(toolWindow.contentManager.factory.createContent(contentPanel, "", false))

        val cardLayout = panel.layout as CardLayout
        cardLayout.show(panel, "Startup")
    }

    private fun openFileInEditor(project: Project, file: VirtualFile, lineNumber: Int, cursorPosition: Int) {
        FileEditorManager.getInstance(project).openTextEditor(
            OpenFileDescriptor(project, file, lineNumber - 1, cursorPosition, false), false
        )
    }

    private fun scanForComments() {
        val cardLayout = panel.layout as CardLayout
        cardLayout.show(panel, "Progress")

        // * Temporary remove listeners to not by accident open old file
        val listeners = commentTree.treeSelectionListeners
        listeners.forEach { commentTree.removeTreeSelectionListener(it) }

        executor.submit {
            val baseDir = VirtualFileManager.getInstance().findFileByUrl("file://${project.basePath}")
            val fileCommentsMap = mutableMapOf<VirtualFile, List<CommentNodeData>>()
            baseDir?.let { scanForComments(project, it, fileCommentsMap) }
            ApplicationManager.getApplication().invokeLater {
                updateTreeModel(fileCommentsMap, project)
                cardLayout.show(panel, "Tree")

                // * Restore listeners
                listeners.forEach { commentTree.addTreeSelectionListener(it) }
            }
        }
    }

    private fun scanForComments(project: Project, directory: VirtualFile, fileCommentsMap: MutableMap<VirtualFile, List<CommentNodeData>>) {
        val changeListManager = ChangeListManager.getInstance(project)
        val scopeType = SearchTypes.fromString(scopeTypeComboBox.selectedItem as String)
        val commentType = commentTypeComboBox.selectedItem as String

        if(foldersToExclude.contains(directory.name)) {
            return
        }

        val filesToScan = when (scopeType) {
            SearchTypes.RecentlyChangedFiles -> {
                changeListManager.allChanges.mapNotNull { it.virtualFile }
            }
            SearchTypes.OpenedFiles -> {
                FileEditorManager.getInstance(project).openFiles.toList()
            }
            SearchTypes.CurrentFile -> {
                FileEditorManager.getInstance(project).selectedFiles.toList()
            }
            else -> directory.children.toList()
        }

        filesToScan.forEach { child ->
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

                        val comments = mutableListOf<CommentNodeData>()
                        psiComments.forEach eachComment@ { comment ->
                            val foundBetterComments = commentsParser.findBetterComments(comment)
                            if(foundBetterComments.isEmpty()) {
                                return@eachComment
                            }

                            foundBetterComments.forEach { betterComment ->
                                if (commentType == allCommentsType || commentType == betterComment.tag.type) {
                                    val lineNumber = document.getLineNumber(betterComment.startOffset) + 1
                                    val cursorPosition = betterComment.startOffset - document.getLineStartOffset(lineNumber - 1)
                                    comments.add(CommentNodeData(betterComment.text, lineNumber, cursorPosition, betterComment.tag))
                                }

                            }
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
                    val node = DefaultMutableTreeNode(FolderNodeData(folderName, 0))
                    rootNode.add(node)
                    node
                }
                folderNode.add(fileNode)
                folderFileCount[folderName] = folderFileCount.getOrDefault(folderName, 0) + 1
            }
        }

        folderNodes.forEach{ (folderName, folderNode) ->
            val fileCount = folderFileCount[folderName] ?: 0
            (folderNode.userObject as FolderNodeData).itemsCounter = fileCount
        }

        val commentsLabel = if (commentsCounter == 1) "comment" else "comments"
        val filesLabel = if (filesCounter == 1) "file" else "files"
        rootNode.userObject = "Found $commentsCounter $commentsLabel in $filesCounter $filesLabel"
        treeModel.reload()
    }

    private fun getComboBoxForCommentsType(project: Project): ComboBox<String> {
        val comboBox = ComboBox<String>()
        comboBox.addItem(allCommentsType)

        val betterCommentSettings = BetterCommentsSettings.getInstance(project).state
        for (type in betterCommentSettings.tags) {
            comboBox.addItem(type.type)
        }

        return comboBox
    }
}