package com.lczerniawski.bettercomments.codehiglighter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import java.util.concurrent.Executors

class HighlightFileManagerListener : FileEditorManagerListener, ProjectManagerListener {
    private val executor = Executors.newSingleThreadExecutor()
    private val documentListeners = mutableMapOf<Document, HighlightDocumentListener>()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val editors = EditorFactory.getInstance().allEditors

        for (editor in editors) {
            val document = editor.document
            val documentListener = HighlightDocumentListener(editor)
            document.addDocumentListener(documentListener)
            documentListeners[document] = documentListener

            executor.submit {
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(editor.project) {
                        CommentsHighlighter.applyCustomHighlighting(editor)
                    }
                }
            }
        }
    }

    override fun projectClosed(project: Project) {
        dispose()
    }

    private fun dispose() {
        for ((document, listener) in documentListeners) {
            document.removeDocumentListener(listener)
        }
        documentListeners.clear()
        executor.shutdown()
    }
}