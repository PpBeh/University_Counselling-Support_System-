// Module 5 domain tests | Owner: Michael
package com.example.counselling.feature.resource

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.StressBracket
import com.example.counselling.feature.resource.domain.ArticleResource
import com.example.counselling.feature.resource.domain.HotlineResource
import com.example.counselling.feature.resource.domain.ResourceDraft
import com.example.counselling.feature.resource.domain.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceDomainTest {

    private fun article() = ArticleResource(
        id = "resource-1",
        title = "Grounding",
        description = "A sufficiently long description.",
        category = ConcernCategory.ANXIETY,
        targetBrackets = setOf(StressBracket.LOW, StressBracket.MODERATE),
        isPublished = true,
        createdAt = null,
        updatedAt = null,
        contentUrl = "https://example.com/grounding",
    )

    @Test
    fun subtypesExposeTheirOwnMediaTypeAndHotlineFlag() {
        val article = article()
        assertEquals("Article", article.mediaType)
        assertFalse(article.isHotline)
        assertTrue(article.targets(StressBracket.LOW))
        assertFalse(article.targets(StressBracket.HIGH))

        val hotline = HotlineResource(
            id = "resource-2",
            title = "Helpline",
            description = "A sufficiently long description.",
            category = ConcernCategory.OTHER,
            targetBrackets = setOf(StressBracket.HIGH),
            isPublished = true,
            createdAt = null,
            updatedAt = null,
            contentUrl = "https://example.com/help",
            phoneNumber = "999",
            operatingHours = "24/7",
        )
        assertEquals("Hotline", hotline.mediaType)
        assertTrue(hotline.isHotline)
    }

    @Test
    fun draftRejectsInvalidUrlAndEmptyBrackets() {
        val base = ResourceDraft(
            title = "Grounding",
            description = "A sufficiently long description.",
            category = ConcernCategory.ANXIETY,
            type = ResourceType.ARTICLE,
            contentUrl = "https://example.com/grounding",
            targetBrackets = setOf(StressBracket.LOW),
            isPublished = true,
        )
        assertEquals(base, base.validate())
        assertThrows(DomainException::class.java) {
            base.copy(contentUrl = "not-a-link").validate()
        }
        assertThrows(DomainException::class.java) {
            base.copy(targetBrackets = emptySet()).validate()
        }
    }
}
