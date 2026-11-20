// Module 6 domain tests | Owner: Michael
package com.example.counselling.feature.enquiry

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Rating
import com.example.counselling.feature.enquiry.domain.AnonymousEnquiry
import com.example.counselling.feature.enquiry.domain.EnquiryStatus
import com.example.counselling.feature.enquiry.domain.Feedback
import com.example.counselling.feature.enquiry.domain.IdentifiedEnquiry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EnquiryDomainTest {

    private fun identified() = IdentifiedEnquiry(
        id = "enquiry-1",
        referenceCode = "ENQ-ABC123",
        category = ConcernCategory.ANXIETY,
        message = "I need help with exam anxiety.",
        status = EnquiryStatus.OPEN,
        reply = null,
        recommendedResourceId = null,
        recommendedResourceTitle = null,
        repliedAt = null,
        createdAt = null,
        ownerId = "student-1",
        ownerName = "Aina",
    )

    @Test
    fun identifiedAndAnonymousEnquiriesExposeDifferentOwnership() {
        val identified = identified()
        assertFalse(identified.isAnonymous)
        assertEquals("student-1", identified.studentId)
        assertEquals("Aina", identified.studentName)
        assertTrue(identified.canReply())

        val anonymous = AnonymousEnquiry(
            id = "enquiry-2",
            referenceCode = "ENQ-DEF456",
            category = ConcernCategory.SLEEP,
            message = "Anonymous question about sleep.",
            status = EnquiryStatus.ANSWERED,
            reply = "Try a wind-down routine.",
            recommendedResourceId = null,
            recommendedResourceTitle = null,
            repliedAt = null,
            createdAt = null,
        )
        assertTrue(anonymous.isAnonymous)
        assertNull(anonymous.studentId)
        assertNull(anonymous.studentName)
    }

    @Test
    fun shortMessagesAndLongCommentsAreRejected() {
        assertThrows(DomainException::class.java) {
            IdentifiedEnquiry(
                id = "x",
                referenceCode = "ENQ-ABC123",
                category = ConcernCategory.OTHER,
                message = "short",
                status = EnquiryStatus.OPEN,
                reply = null,
                recommendedResourceId = null,
                recommendedResourceTitle = null,
                repliedAt = null,
                createdAt = null,
                ownerId = "s",
                ownerName = null,
            )
        }
        assertThrows(DomainException::class.java) {
            Feedback(
                id = "feedback-1",
                appointmentId = "appointment-1",
                studentName = "Aina",
                counsellorName = "Counsellor",
                rating = Rating.of(5),
                comment = "x".repeat(501),
                createdAt = null,
                sessionStart = null,
                concernCategory = "Anxiety",
            )
        }
    }
}
