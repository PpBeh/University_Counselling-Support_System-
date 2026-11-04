// Core | Owner: shared (Beh, Jeremy, Michael)
// Shared constants. These mirror the PostgreSQL checks in supabase/schema.sql.
package com.example.counselling.core

object AppConstants {
    const val APP_NAME = "Counselling Support"
    const val TIME_ZONE = "Asia/Kuala_Lumpur"

    const val MAX_ACTIVE_BOOKINGS = 2
    const val MIN_NOTICE_HOURS = 12
    const val RECORD_EDIT_DAYS = 7

    val concernCategories = listOf(
        "Stress",
        "Anxiety",
        "Academic Pressure",
        "Relationships",
        "Sleep",
        "Financial",
        "Loneliness",
        "Other",
    )

    val sessionModes = listOf("in_person", "online")
    val sessionModeLabels = mapOf(
        "in_person" to "In person",
        "online" to "Online",
    )

    val appointmentStatuses = listOf("pending", "confirmed", "completed", "no_show", "cancelled")
    val riskLevels = listOf("low", "medium", "high")
    val resourceMediaTypes = listOf("Article", "Video", "Audio", "Hotline", "PDF")
    val stressBrackets = listOf("low", "moderate", "high")

    val stressBracketLabels = mapOf(
        "low" to "Low",
        "moderate" to "Moderate",
        "high" to "High",
    )

    val enquiryStatuses = listOf("open", "answered", "closed")
}
