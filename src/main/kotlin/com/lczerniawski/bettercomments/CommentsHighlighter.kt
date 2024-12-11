package com.lczerniawski.bettercomments

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
import java.awt.Font

object CommentsHighlighter {
    private const val CUSTOM_HIGHLIGHTER_LAYER = HighlighterLayer.LAST + 1000

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
        if (comment.tokenType.toString().contains("BLOCK_COMMENT")){
            val text = comment.text
            val lines = text.split("\n")

            for (line in lines) {
                val lineTrimmed = parseBlockComment(line)
                val startOffset = comment.textRange.startOffset + text.indexOf(line)
                val endOffset = startOffset + line.length

                applyCorrectHighlightStyle(lineTrimmed, startOffset, endOffset, markupModel)
            }
        } else {
            val text = comment.text
            val textTrimmed = parseSingleLineComment(text)
            val startOffset = comment.textRange.startOffset
            val endOffset = comment.textRange.endOffset

            applyCorrectHighlightStyle(textTrimmed, startOffset, endOffset, markupModel)
        }
    }

    private fun parseSingleLineComment(comment: String): String {
        val trimmedSpacesText = comment.trimStart()
        val trimmedNonSpecialComments = trimmedSpacesText.trimStart( '#', '-', '\'')
        val trimmedSpecialComments =  trimmedNonSpecialComments.trimStartOnce("//", "*")
        return trimmedSpecialComments.trimStart()
    }

    private fun parseBlockComment(comment: String): String {
        val trimmedSpacesText = comment.trimStart()
        val trimmedNonSpecialComments = trimmedSpacesText.trimStart( '#', '-', '\'')
        val trimmedSpecialCommentsOnce = trimmedNonSpecialComments.trimStartOnce("/*", "*/", "/**", "**/")
        val trimmedSpecialComments = trimmedSpecialCommentsOnce.trimStartOnceIfExistsMoreThanOnce("//", "*")
        return trimmedSpecialComments.trimStart()
    }

    private fun applyCorrectHighlightStyle(comment: String, startOffset: Int, endOffset: Int, markupModel: MarkupModel) {
        val settings = BetterCommentsSettings.instance
        for (tag in settings.tags) {
            if (comment.lowercase().startsWith(tag.type.lowercase())) {

                val attributes = TextAttributes()
                attributes.foregroundColor = JBColor.decode(tag.color)

                if (tag.isBold) {
                    attributes.fontType = attributes.fontType or Font.BOLD
                }

                if (tag.isItalic) {
                    attributes.fontType = attributes.fontType or Font.ITALIC
                }

                if (tag.hasUnderline) {
                    attributes.effectType = EffectType.LINE_UNDERSCORE
                    attributes.effectColor = JBColor.decode(tag.color)
                }

                if (tag.hasStrikethrough) {
                    attributes.effectType = EffectType.STRIKEOUT
                    attributes.effectColor = JBColor.decode(tag.color)
                }

                if (tag.backgroundColor != null) {
                    attributes.backgroundColor = JBColor.decode(tag.backgroundColor)
                }

                markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    CUSTOM_HIGHLIGHTER_LAYER,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
            }
        }
    }

    private fun String.trimStartOnce(vararg strings: String): String {
        if (this.isNotEmpty()) {
            for (string in strings) {
                if (this.startsWith(string)) {
                    return this.substring(string.length)
                }
            }
        }

        return this
    }

    private fun String.trimStartOnceIfExistsMoreThanOnce(vararg strings: String): String {
        if (this.isNotEmpty()) {
            for (string in strings) {
                val patternCount = countPatternExistence(this, string)
                if (patternCount > 1) {
                    return this.substring(string.length)
                }
            }
        }

        return this
    }

    private fun countPatternExistence(input: String, pattern: String): Int {
        var count = 0

        var index = input.indexOf(pattern)
        while (index != -1) {
            count++
            index = input.indexOf(pattern, index + 2)
        }

        return count
    }
}