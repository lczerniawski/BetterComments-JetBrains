package com.lczerniawski.bettercomments.models

data class CustomTag(
    var type: String = "",
    var color: String = "",
    var hasStrikethrough: Boolean = false,
    var hasUnderline: Boolean = false,
    var backgroundColor: String? = null,
    var isBold: Boolean = false,
    var isItalic: Boolean = false
)