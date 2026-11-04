// Module 2 | Owner: Beh
// Models for appointment rows and the public available-slots view. The view
// hides booked slots without exposing any other student's appointment.
package com.example.counselling.feature.appointment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("slot_id") val slotId: String,
    val mode: String,
    @SerialName("concern_category") val concernCategory: String,
    val note: String? = null,
    val status: String,
    @SerialName("follow_up_of") val followUpOf: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AvailabilitySlotRow(
    val id: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
)

@Serializable
data class CounsellorSummaryRow(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
)

@Serializable
data class AvailableSlotRow(
    @SerialName("slot_id") val slotId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("counsellor_name") val counsellorName: String,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
)

/** A joined appointment ready for display; assembled in the repository. */
data class Appointment(
    val id: String,
    val studentId: String,
    val counsellorId: String,
    val slotId: String,
    val mode: String,
    val concernCategory: String,
    val note: String?,
    val status: String,
    val followUpOf: String?,
    val createdAt: String?,
    val slotStart: String,
    val slotEnd: String,
    val counsellorName: String,
    val counsellorSpecialisations: List<String>,
    val studentName: String,
    val studentEmail: String?,
    val studentCode: String?,
    val studentPhone: String?,
) {
    val isFollowUp: Boolean get() = followUpOf != null
}

/** Smart Counsellor Match result: one counsellor plus their free slots. */
data class CounsellorMatch(
    val counsellorId: String,
    val counsellorName: String,
    val specialisations: List<String>,
    val languages: List<String>,
    val bio: String,
    val nextSlotStart: String,
    val slots: List<AvailableSlotRow>,
)
