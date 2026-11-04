// Core | Owner: shared (Beh, Jeremy, Michael)
// The three roles supported by the app. The database remains the authority;
// this enum only drives presentation and navigation after the role is loaded.
package com.example.counselling.core

enum class UserRole(val wireValue: String, val displayName: String) {
    STUDENT("student", "Student"),
    COUNSELLOR("counsellor", "Counsellor"),
    ADMIN("admin", "Administrator");

    companion object {
        fun fromWire(value: String?): UserRole? =
            entries.firstOrNull { it.wireValue == value }
    }
}
