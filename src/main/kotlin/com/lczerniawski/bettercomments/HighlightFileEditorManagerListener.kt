package com.lczerniawski.bettercomments

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener

class HighlightFileEditorManagerListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val editors = EditorFactory.getInstance().allEditors

        for (editor in editors) {
            val document = editor.document
            val documentListener = HighlightDocumentListener(editor)
            document.addDocumentListener(documentListener)

            WriteCommandAction.runWriteCommandAction(editor.project) {
                CommentsHighlighter.applyCustomHighlighting(editor)
            }
        }
    }
}