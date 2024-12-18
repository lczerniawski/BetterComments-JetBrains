package com.lczerniawski.bettercomments.settings

class DefaultTags(val type: String, val color: String, val hasStrikethrough: Boolean, val hasUnderline: Boolean, val backgroundColor: String?, val isBold: Boolean, val isItalic: Boolean) {
    companion object {
        val Alert = DefaultTags("!", "#FF2D00", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Query = DefaultTags("?", "#3498DB", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val CommentedOut = DefaultTags("//", "#474747", hasStrikethrough = true, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Todo = DefaultTags("todo", "#FF8C00", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Information = DefaultTags("*", "#98C379", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)

        val values = listOf(Alert, Query, CommentedOut, Todo, Information)
    }
}