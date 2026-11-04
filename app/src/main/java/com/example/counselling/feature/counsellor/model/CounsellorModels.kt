// Module 3 | Owner: Jeremy
// Counsellor directory and availability models.
package com.example.counselling.feature.counsellor.model

import com.example.counselling.core.DateTimeUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CounsellorRow(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AvailabilitySlotRow(
    val id: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AppointmentSlotUsageRow(
    @SerialName("slot_id") val slotId: String,
    val status: String,
)

@Serializable
data class CounsellorInsert(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val specialisations: List<String>,
    val languages: List<String>,
    val bio: String,
    @SerialName("is_active") val isActive: Boolean = true,
)

data class AvailabilitySlot(
    val id: String,
    val counsellorId: String,
    val startAt: String,
    val endAt: String,
    val isBooked: Boolean = false,
) {
    val isPast: Boolean get() = DateTimeUtils.isPast(startAt)
    val isEditable: Boolean get() = !isBooked && !isPast
}

data class Counsellor(
    val id: String,
    val userId: String?,
    val fullName: String,
    val email: String,
    val specialisations: List<String>,
    val languages: List<String>,
    val bio: String,
    val isActive: Boolean,
    val availableThisWeek: Boolean = false,
    val slots: List<AvailabilitySlot> = emptyList(),
) {
    val upcomingFreeSlots: List<AvailabilitySlot>
        get() = slots.filter { it.isEditable }
}
