// Module 6 | Owner: Michael
package com.example.counselling.feature.enquiry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnquiryRow(
    val id: String,
    @SerialName("reference_code") val referenceCode: String,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    val category: String,
    val message: String,
    val status: String,
    val reply: String? = null,
    @SerialName("recommended_resource_id") val recommendedResourceId: String? = null,
    @SerialName("replied_at") val repliedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class EnquiryLookupRow(
    @SerialName("reference_code") val referenceCode: String,
    val category: String,
    val message: String,
    val reply: String? = null,
    val status: String,
    @SerialName("recommended_resource_id") val recommendedResourceId: String? = null,
    @SerialName("recommended_resource_title") val recommendedResourceTitle: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("replied_at") val repliedAt: String? = null,
)

data class Enquiry(
    val id: String,
    val referenceCode: String,
    val studentId: String?,
    val isAnonymous: Boolean,
    val category: String,
    val message: String,
    val status: String,
    val reply: String?,
    val recommendedResourceId: String?,
    val recommendedResourceTitle: String?,
    val repliedAt: String?,
    val createdAt: String?,
    val studentName: String?,
)

@Serializable
data class FeedbackRow(
    val id: String,
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class FeedbackInsert(
    @SerialName("appointment_id") val appointmentId: String,
    val rating: Int,
    val comment: String? = null,
)

data class FeedbackWithAppointment(
    val feedback: FeedbackRow,
    val appointment: com.example.counselling.feature.appointment.model.Appointment?,
)
