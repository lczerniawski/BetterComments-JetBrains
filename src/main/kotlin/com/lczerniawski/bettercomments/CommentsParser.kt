package com.lczerniawski.bettercomments

import com.intellij.psi.PsiComment
import com.lczerniawski.bettercomments.models.CommentData
import com.lczerniawski.bettercomments.models.CustomTag

class CommentsParser {
    private val settings = BetterCommentsSettings.instance

    fun findBetterComments(comment: PsiComment): List<CommentData> {
        val result = mutableListOf<CommentData>()

        if (comment.tokenType.toString().contains("BLOCK_COMMENT")){
            val text = comment.text
            val lines = text.split("\n")

            for (line in lines) {
                val lineTrimmed = parseBlockComment(line)
                val startOffset = comment.textRange.startOffset + text.indexOf(line)
                val endOffset = startOffset + line.length
                val tag = findTag(lineTrimmed) ?: return result

                result.add(CommentData(lineTrimmed, startOffset, endOffset, tag))
            }
        } else {
            val text = comment.text
            val textTrimmed = parseSingleLineComment(text)
            val startOffset = comment.textRange.startOffset
            val endOffset = comment.textRange.endOffset
            val tag = findTag(textTrimmed) ?: return result

            result.add(CommentData(textTrimmed, startOffset, endOffset, tag))
        }

        return result
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