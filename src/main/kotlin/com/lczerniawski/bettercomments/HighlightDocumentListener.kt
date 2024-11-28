package com.lczerniawski.bettercomments

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.psi.PsiDocumentManager


class HighlightDocumentListener(private val editor: Editor) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        val project = editor.project ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            PsiDocumentManager.getInstance(project).commitDocument(event.document)
            CommentsHighlighter.applyCustomHighlighting(editor)
        }
    }

}