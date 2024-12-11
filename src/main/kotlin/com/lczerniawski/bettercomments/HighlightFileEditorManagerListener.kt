package com.lczerniawski.bettercomments

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import java.util.concurrent.Executors

class HighlightFileEditorManagerListener : FileEditorManagerListener {
    private val executor = Executors.newSingleThreadExecutor()

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val editors = EditorFactory.getInstance().allEditors

        for (editor in editors) {
            val document = editor.document
            val documentListener = HighlightDocumentListener(editor)
            document.addDocumentListener(documentListener)

            executor.submit {
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(editor.project) {
                        CommentsHighlighter.applyCustomHighlighting(editor)
                    }
                }
            }
        }
    }
}