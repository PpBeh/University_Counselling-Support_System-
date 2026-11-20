// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.domain

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.DomainException
import java.time.Instant

// OOP: State - enquiry lifecycle is an enum with explicit display labels.
enum class EnquiryStatus(val code: String, val label: String) {
    OPEN("open", "Open"),
    ANSWERED("answered", "Answered"),
    CLOSED("closed", "Closed"),
    ;

    companion object {
        fun fromWire(value: String?): EnquiryStatus? {
            val text = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.code == text || it.label.lowercase() == text }
        }
    }
}

// OOP: Inheritance - Enquiry is the abstract supertype; identified and
// anonymous enquiries are different subtypes with different ownership data.
sealed class Enquiry(
    val id: String,
    val referenceCode: String,
    val category: ConcernCategory,
    val message: String,
    val status: EnquiryStatus,
    val reply: String?,
    val recommendedResourceId: String?,
    val recommendedResourceTitle: String?,
    val repliedAt: Instant?,
    val createdAt: Instant?,
) {
    abstract val isAnonymous: Boolean
    abstract val studentId: String?
    abstract val studentName: String?

    fun canReply(): Boolean = status != EnquiryStatus.CLOSED

    init {
        if (referenceCode.isBlank()) throw DomainException("An enquiry needs a reference code.")
        if (message.trim().length !in 10..1000) {
            throw DomainException("An enquiry message must be between 10 and 1,000 characters.")
        }
    }
}

class IdentifiedEnquiry(
    id: String,
    referenceCode: String,
    category: ConcernCategory,
    message: String,
    status: EnquiryStatus,
    reply: String?,
    recommendedResourceId: String?,
    recommendedResourceTitle: String?,
    repliedAt: Instant?,
    createdAt: Instant?,
    val ownerId: String,
    val ownerName: String?,
) : Enquiry(
    id, referenceCode, category, message, status, reply,
    recommendedResourceId, recommendedResourceTitle, repliedAt, createdAt,
) {
    override val isAnonymous: Boolean get() = false
    override val studentId: String? get() = ownerId
    override val studentName: String? get() = ownerName
}

class AnonymousEnquiry(
    id: String,
    referenceCode: String,
    category: ConcernCategory,
    message: String,
    status: EnquiryStatus,
    reply: String?,
    recommendedResourceId: String?,
    recommendedResourceTitle: String?,
    repliedAt: Instant?,
    createdAt: Instant?,
) : Enquiry(
    id, referenceCode, category, message, status, reply,
    recommendedResourceId, recommendedResourceTitle, repliedAt, createdAt,
) {
    override val isAnonymous: Boolean get() = true
    override val studentId: String? get() = null
    override val studentName: String? get() = null
}

// OOP: Value object - the public code lookup returns no identity fields.
class EnquiryLookup(
    val referenceCode: String,
    val category: ConcernCategory,
    val message: String,
    val reply: String?,
    val status: EnquiryStatus,
    val recommendedResourceTitle: String?,
    val createdAt: Instant?,
    val repliedAt: Instant?,
)
