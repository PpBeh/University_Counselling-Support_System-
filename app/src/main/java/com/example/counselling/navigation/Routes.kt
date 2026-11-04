// Navigation | Owner: shared (Beh, Jeremy, Michael)
// String routes keep this student project easy to read and explain.
package com.example.counselling.navigation

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
    const val BOOK_APPOINTMENT = "book_appointment"
    const val STUDENT_DIRECTORY = "student_directory"
    const val STUDENT_DETAIL = "student_detail/{studentId}"
    const val RESCHEDULE = "reschedule/{appointmentId}"

    fun studentDetail(studentId: String) = "student_detail/$studentId"
    fun reschedule(appointmentId: String) = "reschedule/$appointmentId"
}
