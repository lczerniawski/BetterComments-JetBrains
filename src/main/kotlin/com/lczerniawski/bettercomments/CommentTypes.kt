package com.lczerniawski.bettercomments

class CommentType(val type: String, val color: String, val hasStrikethrough: Boolean, val hasUnderline: Boolean, val backgroundColor: String?, val isBold: Boolean, val isItalic: Boolean) {
    companion object {
        val Alert = CommentType("!", "#FF2D00", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Query = CommentType("?", "#3498DB", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val CommentedOut = CommentType("//", "#474747", hasStrikethrough = true, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Todo = CommentType("todo", "#FF8C00", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)
        val Information = CommentType("*", "#98C379", hasStrikethrough = false, hasUnderline = false, backgroundColor = null, isBold = false, isItalic = false)

        val values = listOf(Alert, Query, CommentedOut, Todo, Information)
    }
}