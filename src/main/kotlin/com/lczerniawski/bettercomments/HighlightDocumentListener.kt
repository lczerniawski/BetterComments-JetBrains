package com.lczerniawski.bettercomments

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.Editor


class HighlightDocumentListener(private val editor: Editor) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        CommentsHighlighter.applyCustomHighlighting(editor)
    }

}