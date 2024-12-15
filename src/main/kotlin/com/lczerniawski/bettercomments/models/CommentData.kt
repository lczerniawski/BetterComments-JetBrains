package com.lczerniawski.bettercomments.models

data class CommentData(val text: String, val startOffset: Int, val endOffset: Int, val tag: CustomTag)
