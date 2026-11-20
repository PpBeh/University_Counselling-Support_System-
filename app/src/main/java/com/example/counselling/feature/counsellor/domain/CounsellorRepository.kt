// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.domain

import com.example.counselling.core.domain.AvailabilitySlot
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.TimeSlot

// OOP: Abstraction - counsellor/availability ViewModels depend on this contract.
interface CounsellorRepository {

    suspend fun getCounsellors(): Result<List<Counsellor>>

    suspend fun getCounsellor(counsellorId: String): Result<Counsellor?>

    suspend fun getMyCounsellor(authUserId: String): Result<Counsellor?>

    suspend fun createCounsellor(
        fullName: String,
        email: Email,
        specialisations: List<ConcernCategory>,
        languages: List<String>,
        bio: String,
    ): Result<Counsellor>

    suspend fun updateCounsellor(
        counsellorId: String,
        fullName: String,
        email: Email,
        specialisations: List<ConcernCategory>,
        languages: List<String>,
        bio: String,
        isActive: Boolean,
    ): Result<Counsellor>

    suspend fun removeCounsellor(counsellorId: String): Result<CounsellorRemovalResult>

    suspend fun getAvailability(counsellorId: String): Result<List<AvailabilitySlot>>

    suspend fun createSlots(
        counsellorId: String,
        slots: List<TimeSlot>,
    ): Result<Unit>

    suspend fun updateSlot(
        slotId: String,
        slot: TimeSlot,
    ): Result<Unit>

    suspend fun deleteSlot(slotId: String): Result<Unit>
}

// OOP: Sealed result - the caller must handle both deletion outcomes.
sealed interface CounsellorRemovalResult {
    data object Deleted : CounsellorRemovalResult
    data object Deactivated : CounsellorRemovalResult
}
