package com.lczerniawski.bettercomments.common

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.lczerniawski.bettercomments.models.CustomTag
import com.lczerniawski.bettercomments.settings.BetterCommentsGlobalSettings
import com.lczerniawski.bettercomments.settings.BetterCommentsProjectSettings
import com.lczerniawski.bettercomments.settings.BetterCommentsSettings
import org.jdom.Element

class CommentsParserTest : BasePlatformTestCase() {
    fun testLineCommentContinuationBoundaries() {
        val projectSettings = BetterCommentsProjectSettings.getInstance(project)
        val originalProjectState = projectSettings.state
        try {
            projectSettings.isInitialized = true
            projectSettings.tags = mutableListOf(
                CustomTag(type = "NOTE"),
                CustomTag(type = "TODO", highlightContinuations = false)
            )
            val file = myFixture.configureByText(
                "example.json",
                """
                // NOTE tagged line
                //  continuation
                //  another continuation
                // not a continuation
                //  does not inherit NOTE after the reset
                //  TODO explicitly tagged line
                //  does not inherit TODO or fall back to NOTE
                /* TODO tagged block
                 * block continuation
                 */
                """.trimIndent()
            )
            val comments = PsiTreeUtil.collectElementsOfType(file, PsiComment::class.java).toList()
            val parser = CommentsParser(project)
            assertEquals(
                listOf("NOTE", "NOTE", "NOTE", null, null, "TODO", null, "TODO"),
                comments.map { parser.findBetterComments(it).singleOrNull()?.tag?.type }
            )
        } finally {
            projectSettings.loadState(originalProjectState)
        }
    }

    fun testContinuationPreferenceDefaultsOnAndPersistsPerTag() {
        val existingTag = XmlSerializer.deserialize(Element("CustomTag"), CustomTag::class.java)
        assertTrue(existingTag.highlightContinuations)

        val settings = BetterCommentsGlobalSettings()
        settings.tags = mutableListOf(existingTag, CustomTag(type = "TODO", highlightContinuations = false))
        val saved = XmlSerializer.serialize(settings.state)
        val restored = BetterCommentsGlobalSettings()
        restored.loadState(XmlSerializer.deserialize(saved, BetterCommentsSettings.State::class.java))
        assertEquals(listOf(true, false), restored.tags.map { it.highlightContinuations })
    }
}
