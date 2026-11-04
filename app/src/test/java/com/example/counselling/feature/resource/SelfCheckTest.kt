// Module 5 / Unique Feature 3 | Owner: Michael
package com.example.counselling.feature.resource

import com.example.counselling.feature.resource.model.ResourceRow
import com.example.counselling.feature.resource.model.SelfCheckContent
import com.example.counselling.feature.resource.model.recommendResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfCheckTest {

    @Test
    fun brackets_matchSpecifiedRanges() {
        assertEquals("low", SelfCheckContent.bracketFor(0).key)
        assertEquals("low", SelfCheckContent.bracketFor(13).key)
        assertEquals("moderate", SelfCheckContent.bracketFor(14).key)
        assertEquals("moderate", SelfCheckContent.bracketFor(26).key)
        assertEquals("high", SelfCheckContent.bracketFor(27).key)
        assertEquals("high", SelfCheckContent.bracketFor(40).key)
    }

    @Test
    fun dominantConcern_usesHighestCategoryTotal() {
        val answers = listOf(1, 4, 1, 1, 1, 0, 0, 0, 0, 0)
        val result = SelfCheckContent.resultFor(answers)
        assertEquals(8, result.score)
        assertEquals("Anxiety", result.dominantConcern)
    }

    @Test
    fun recommendations_keepPublishedBracketMatchesAndHideHotlineForModerate() {
        val result = SelfCheckContent.resultFor(List(10) { 2 }) // 20, moderate, anxiety dominant
        val resources = listOf(
            resource("hotline", "Hotline", "Other", listOf("moderate", "high"), true),
            resource("published", "Article", "Anxiety", listOf("moderate"), true),
            resource("draft", "Article", "Anxiety", listOf("moderate"), false),
            resource("other-bracket", "Article", "Sleep", listOf("high"), true),
        )

        val recommended = recommendResources(resources, result)

        assertEquals(listOf("published"), recommended.map { it.title })
        assertFalse(recommended.any { it.isHotline })
    }

    @Test
    fun recommendations_showHotlineForHighAndPrioritiseDominantCategory() {
        val result = SelfCheckContent.resultFor(List(10) { 3 }) // 30, high, anxiety dominant
        val resources = listOf(
            resource("sleep", "Article", "Sleep", listOf("high"), true),
            resource("hotline", "Hotline", "Other", listOf("high"), true),
            resource("anxiety", "Article", "Anxiety", listOf("high"), true),
        )

        val recommended = recommendResources(resources, result)

        assertEquals("anxiety", recommended.first().title)
        assertTrue(recommended.any { it.isHotline })
    }

    private fun resource(
        title: String,
        mediaType: String,
        category: String,
        brackets: List<String>,
        published: Boolean,
    ) = ResourceRow(
        id = title,
        title = title,
        description = "A sufficiently long description for testing.",
        category = category,
        mediaType = mediaType,
        contentUrl = "https://example.com/$title",
        targetStressBrackets = brackets,
        isPublished = published,
    )
}
