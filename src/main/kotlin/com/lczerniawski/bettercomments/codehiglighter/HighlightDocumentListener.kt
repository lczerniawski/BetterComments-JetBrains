package com.lczerniawski.bettercomments.codehiglighter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.psi.PsiDocumentManager
import java.util.concurrent.Executors

class HighlightDocumentListener(private val editor: Editor) : DocumentListener {
    private val executor = Executors.newSingleThreadExecutor()

    override fun documentChanged(event: DocumentEvent) {
        val project = editor.project ?: return

        if (project.isDisposed) {
            return
        }

        executor.submit {
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    PsiDocumentManager.getInstance(project).commitDocument(event.document)
                    CommentsHighlighter.applyCustomHighlighting(editor)
                }
            }
        }
    }
}