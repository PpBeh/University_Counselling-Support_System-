// Navigation | Owner: shared (Beh, Jeremy, Michael)
// String routes keep this student project easy to read and explain.
package com.example.counselling.navigation

import android.net.Uri

object Routes {
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main tabs
    const val HOME = "home"
    const val PROFILE = "profile"
    const val STUDENT_APPOINTMENTS = "student_appointments"
    const val ENQUIRIES = "enquiries"
    const val RESOURCES = "resources"
    const val COUNSELLOR_SCHEDULE = "counsellor_schedule"
    const val COUNSELLOR_RECORDS = "counsellor_records"
    const val ADMIN_COUNSELLORS = "admin_counsellors"
    const val ADMIN_STUDENTS = "admin_students"

    // Detail/secondary destinations
    const val SELF_CHECK = "wellbeing_selfcheck"
    const val RESOURCES_HOTLINE = "resources_hotline"
    const val ADMIN_APPOINTMENTS = "admin_appointments"
    const val BOOK_APPOINTMENT = "book_appointment?concern={concern}"
    const val RESOURCE_FORM = "resource_form?resourceId={resourceId}"
    const val STUDENT_FEEDBACK = "student_feedback"
    const val COUNSELLOR_FEEDBACK = "counsellor_feedback"
    const val STUDENT_DIRECTORY = "student_directory"
    const val STUDENT_DETAIL = "student_detail/{studentId}"
    const val RESCHEDULE = "reschedule/{appointmentId}"
    const val COUNSELLOR_RESCHEDULE = "counsellor_reschedule/{appointmentId}"

    const val COUNSELLOR_DETAIL = "counsellor_detail/{counsellorId}"
    const val COUNSELLOR_FORM = "counsellor_form?counsellorId={counsellorId}"
    const val COUNSELLOR_AVAILABILITY = "counsellor_availability/{counsellorId}"
    const val MY_AVAILABILITY = "my_availability"

    const val SESSION_RECORD_FORM =
        "session_record_form?recordId={recordId}&appointmentId={appointmentId}"
    const val FOLLOW_UP = "follow_up/{recordId}"
    const val STUDENT_SUMMARIES = "student_summaries"

    fun studentDetail(studentId: String) = "student_detail/$studentId"
    fun reschedule(appointmentId: String) = "reschedule/$appointmentId"
    fun counsellorReschedule(appointmentId: String) = "counsellor_reschedule/$appointmentId"
    fun bookAppointment(concern: String? = null): String =
        if (concern == null) "book_appointment" else "book_appointment?concern=${Uri.encode(concern)}"
    fun resourceForm(resourceId: String? = null): String =
        if (resourceId == null) "resource_form" else "resource_form?resourceId=$resourceId"
    fun counsellorDetail(counsellorId: String) = "counsellor_detail/$counsellorId"
    fun counsellorForm(counsellorId: String? = null) =
        if (counsellorId == null) "counsellor_form" else "counsellor_form?counsellorId=$counsellorId"
    fun counsellorAvailability(counsellorId: String) =
        "counsellor_availability/$counsellorId"
    fun sessionRecordForm(recordId: String? = null, appointmentId: String? = null): String {
        val query = when {
            recordId != null -> "?recordId=$recordId"
            appointmentId != null -> "?appointmentId=$appointmentId"
            else -> ""
        }
        return "session_record_form$query"
    }
    fun followUp(recordId: String) = "follow_up/$recordId"
}
