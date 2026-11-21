package com.lczerniawski.bettercomments.common

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.lczerniawski.bettercomments.models.CommentData
import com.lczerniawski.bettercomments.models.CustomTag
import com.lczerniawski.bettercomments.settings.BetterCommentsSettings
import kotlin.text.trimStart

class CommentsParser(project: Project) {
    private val hashBangComment = "#!/"
    private val blockCommentTypes = setOf("BLOCK_COMMENT", "C_STYLE_COMMENT")
    private val settings = BetterCommentsSettings.getSettings(project)

    fun findBetterComments(comment: PsiComment): List<CommentData> {
        if (comment.text.startsWith(hashBangComment)) {
            return emptyList()
        }

        return if (blockCommentTypes.contains(comment.tokenType.toString())) {
            processBlockComment(comment)
        } else {
            processLineComment(comment)
        }
    }

    private fun processLineComment(comment: PsiComment): List<CommentData> {
        val text = comment.text
        val textTrimmed = parseSingleLineComment(text)
        val tag = findTag(textTrimmed) ?: findTagFromPreviousAdjacent(comment) ?: return emptyList()

        return listOf(CommentData(textTrimmed, comment.textRange.startOffset, comment.textRange.endOffset, tag))
    }

    private fun processBlockComment(
        comment: PsiComment
    ): List<CommentData> {
        val text = comment.text
        val lines = text.split("\n")

        if (lines.isEmpty()) {
            return emptyList()
        }

        val lineComments = lines.mapNotNull { line ->
            val lineTrimmed = parseBlockComment(line)
            findTag(lineTrimmed)?.let { tag ->
                val startOffset = comment.textRange.startOffset + text.indexOf(line)
                val endOffset = startOffset + line.length

                CommentData(lineTrimmed, startOffset, endOffset, tag)
            }
        }

        val uniqueTags = lineComments.distinctBy { it.tag.type }
        return if (uniqueTags.size == 1) {
            listOf(
                CommentData(
                    text,
                    comment.textRange.startOffset,
                    comment.textRange.endOffset,
                    uniqueTags.first().tag
                )
            )
        } else {
            lineComments
        }
    }

    private fun findTag(comment: String): CustomTag? {
        for (tag in settings.tags) {
            if (comment.lowercase().startsWith(tag.type.lowercase())) {
                return tag
            }
        }

        return null
    }

    private fun parseSingleLineComment(comment: String): String {
        val trimmedSpecialComments = trimSingleLineChars(comment)
        return trimmedSpecialComments.trimStart()
    }

    private fun parseBlockComment(comment: String): String {
        val trimmedSpacesText = comment.trimStart()
        val trimmedNonSpecialComments = trimmedSpacesText.trimStart('#', '-', '\'')
        val trimmedSpecialCommentsOnce = trimmedNonSpecialComments.trimStartOnce("/*", "*/", "/**", "**/", "<!--")
        val trimmedSpecialComments = trimmedSpecialCommentsOnce.trimStartOnceIfExistsMoreThanOnce("//", "*")
        return trimmedSpecialComments.trimStart()
    }

    private fun trimSingleLineChars(comment: String): String {
        val trimmedSpacesText = comment.trimStart()
        val trimmedNonSpecialComments = trimmedSpacesText.trimStart('#', '-', '\'')
        return trimmedNonSpecialComments.trimStartOnce("//", "*", "<!--")
    }

    private fun isContinuation(text: String): Boolean = text.startsWith("  ") || text.isEmpty()

    private fun findTagFromPreviousAdjacent(comment: PsiComment): CustomTag? {
        if (!isContinuation(trimSingleLineChars(comment.text))) return null

        var previous = comment.prevSibling
        while (previous != null) {
            if (previous.text.contains("\n\n") || previous.text.count { it == '\n' } > 1) return null

            if (previous is PsiComment) {
                if (previous.tokenType.toString().contains("BLOCK_COMMENT")) return null

                val text = trimSingleLineChars(previous.text)
                val tag = findTag(text.trimStart())
                if (tag != null) return tag.takeIf { it.highlightContinuations }
                if (!isContinuation(text)) return null
            }

            previous = previous.prevSibling
        }

        return null
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