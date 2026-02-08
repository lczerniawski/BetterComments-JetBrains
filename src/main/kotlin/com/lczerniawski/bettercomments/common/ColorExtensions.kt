package com.lczerniawski.bettercomments.common

import com.intellij.ui.JBColor
import java.awt.Color

fun Color?.toHexWithAlpha(): String {
    return if (this != null) {
        String.format("#%06X%02X", this.rgb and 0xFFFFFF, this.alpha)
    } else {
        ""
    }
}

fun String.parseColorWithAlpha(defaultColor: Color = JBColor.WHITE): Color {
    return try {
        when (this.length) {
            7 -> Color.decode(this) // #RRGGBB - defaults to alpha 255
            9 -> { // #RRGGBBAA
                val rgb = this.substring(1, 7).toInt(16)
                val alpha = this.substring(7, 9).toInt(16)
                Color(
                    (rgb shr 16) and 0xFF,
                    (rgb shr 8) and 0xFF,
                    rgb and 0xFF,
                    alpha
                )
            }
            else -> defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}


