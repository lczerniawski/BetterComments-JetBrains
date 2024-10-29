package com.lczerniawski.bettercomments

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Color

object CommentsHighlighter {
    fun applyCustomHighlighting(editor: Editor) {
        val project = editor.project ?: return
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val markupModel = editor.markupModel
        markupModel.removeAllHighlighters()

        val comments = PsiTreeUtil.collectElementsOfType(psiFile, PsiComment::class.java)
        for (comment in comments) {
            highlightComment(comment, markupModel)
        }
    }

    private fun highlightComment(comment: PsiComment, markupModel: com.intellij.openapi.editor.markup.MarkupModel) {
        val startOffset = comment.textRange.startOffset
        val endOffset = comment.textRange.endOffset

        val attributes = TextAttributes()
        attributes.foregroundColor = Color.RED

        markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.LAST,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
        )
    }
}