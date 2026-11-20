// Core infrastructure | Owner: shared (Beh, Jeremy, Michael)
// One read-model DTO for the three user subtype views. Defaults keep the
// student/counsellor/administrator variants compatible with a single decoder.
package com.example.counselling.core.data

import com.example.counselling.core.domain.UserRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OOP: DTO - transport/read shape only; UserFactory turns it into a domain User.
@Serializable
data class UserViewRow(
    val id: String,
    @SerialName("auth_user_id") val authUserId: String? = null,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("user_type") val userType: String,
    @SerialName("student_id") val studentId: String? = null,
    val programme: String? = null,
    @SerialName("preferred_session_mode") val preferredSessionMode: String? = null,
    @SerialName("preferred_session_mode_label") val preferredSessionModeLabel: String? = null,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toRecord(): UserRecord = UserRecord(
        id = id,
        email = email,
        fullName = fullName,
        phone = phone,
        isActive = isActive,
        authUserId = authUserId,
        userType = userType,
        studentId = studentId,
        programme = programme,
        preferredSessionMode = preferredSessionMode,
        specialisations = specialisations,
        languages = languages,
        bio = bio,
    )
}
