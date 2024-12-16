package com.lczerniawski.bettercomments.codehiglighter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.lczerniawski.bettercomments.common.CommentsParser
import java.awt.Font

object CommentsHighlighter {
    private const val CUSTOM_HIGHLIGHTER_LAYER = HighlighterLayer.LAST + 1000
    private val commentsParser = CommentsParser()

    fun applyCustomHighlighting(editor: Editor) {
        val project = editor.project ?: return
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val markupModel = editor.markupModel
        for (highlighter in markupModel.allHighlighters) {
            if (highlighter.layer == CUSTOM_HIGHLIGHTER_LAYER) {
                markupModel.removeHighlighter(highlighter)
            }
        }

        val comments = PsiTreeUtil.collectElementsOfType(psiFile, PsiComment::class.java)
        for (comment in comments) {
            highlightComment(comment, markupModel)
        }
    }

    private fun highlightComment(comment: PsiComment, markupModel: MarkupModel) {
        val comments = commentsParser.findBetterComments(comment)
        comments.forEach { parsedComment ->
            val attributes = TextAttributes()
            attributes.foregroundColor = JBColor.decode(parsedComment.tag.color)

            if (parsedComment.tag.isBold) {
                attributes.fontType = attributes.fontType or Font.BOLD
            }

            if (parsedComment.tag.isItalic) {
                attributes.fontType = attributes.fontType or Font.ITALIC
            }

            if (parsedComment.tag.hasUnderline) {
                attributes.effectType = EffectType.LINE_UNDERSCORE
                attributes.effectColor = JBColor.decode(parsedComment.tag.color)
            }

            if (parsedComment.tag.hasStrikethrough) {
                attributes.effectType = EffectType.STRIKEOUT
                attributes.effectColor = JBColor.decode(parsedComment.tag.color)
            }

            if (parsedComment.tag.backgroundColor != null) {
                attributes.backgroundColor = JBColor.decode(parsedComment.tag.backgroundColor)
            }

            markupModel.addRangeHighlighter(
                parsedComment.startOffset,
                parsedComment.endOffset,
                CUSTOM_HIGHLIGHTER_LAYER,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }
}