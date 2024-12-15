package com.lczerniawski.bettercomments.models

data class CommentNodeData(val text: String, val lineNumber: Int, val cursorPosition: Int, val tag: CustomTag)
