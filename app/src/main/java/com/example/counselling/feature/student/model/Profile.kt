// Module 1 | Owner: Beh
// Mirrors one row from public.profiles. Passwords and auth tokens are never
// part of this model.
package com.example.counselling.feature.student.model

import com.example.counselling.core.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_id") val studentId: String? = null,
    val programme: String? = null,
    val phone: String? = null,
    @SerialName("preferred_session_mode") val preferredSessionMode: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
) {
    val userRole: UserRole? get() = UserRole.fromWire(role)

    val initials: String
        get() {
            val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            }
        }
}
