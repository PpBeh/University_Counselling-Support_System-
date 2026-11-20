// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.domain

import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.ReferenceCode

// OOP: Abstraction - enquiry ViewModels only know this contract.
interface EnquiryRepository {

    suspend fun getMyEnquiries(): Result<List<Enquiry>>

    suspend fun getInbox(): Result<List<Enquiry>>

    suspend fun submitEnquiry(
        category: ConcernCategory,
        message: String,
        isAnonymous: Boolean,
    ): Result<String>

    suspend fun lookupByCode(code: ReferenceCode): Result<EnquiryLookup?>

    suspend fun withdrawByCode(code: ReferenceCode): Result<Unit>

    suspend fun deleteOwnLinkedEnquiry(enquiryId: String): Result<Unit>

    suspend fun reply(
        enquiryId: String,
        reply: String,
        status: EnquiryStatus,
        resourceId: String?,
    ): Result<Unit>
}
