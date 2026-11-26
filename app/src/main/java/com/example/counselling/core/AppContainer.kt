// Core | Owner: shared (Beh, Jeremy, Michael)
// OOP: Composition root / manual dependency injection - this class is the only
// place that constructs concrete Supabase repositories. ViewModels receive the
// repository interfaces, so feature code never creates its own data source.
package com.example.counselling.core

import android.content.Context
import com.example.counselling.BuildConfig
import com.example.counselling.core.data.SupabaseUserRepository
import com.example.counselling.core.domain.UserRepository
import com.example.counselling.core.settings.ThemePreferences
import com.example.counselling.feature.appointment.data.SupabaseAppointmentRepository
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.auth.data.SupabaseAuthRepository
import com.example.counselling.feature.auth.domain.AuthRepository
import com.example.counselling.feature.counsellor.data.SupabaseCounsellorRepository
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import com.example.counselling.feature.enquiry.data.SupabaseEnquiryRepository
import com.example.counselling.feature.enquiry.data.SupabaseFeedbackRepository
import com.example.counselling.feature.enquiry.domain.EnquiryRepository
import com.example.counselling.feature.enquiry.domain.FeedbackRepository
import com.example.counselling.feature.records.data.SupabaseSessionRecordRepository
import com.example.counselling.feature.records.domain.SessionRecordRepository
import com.example.counselling.feature.resource.data.SupabaseResourceRepository
import com.example.counselling.feature.resource.domain.ResourceRepository
import com.example.counselling.feature.student.data.SupabaseStudentRepository
import com.example.counselling.feature.student.domain.StudentRepository
import io.github.jan.supabase.SupabaseClient

class AppContainer(context: Context) {

    val themePreferences = ThemePreferences(context)

    val configurationError: String?

    val supabase: SupabaseClient?
    val authRepository: AuthRepository?
    val userRepository: UserRepository?
    val studentRepository: StudentRepository?
    val appointmentRepository: AppointmentRepository?
    val counsellorRepository: CounsellorRepository?
    val recordRepository: SessionRecordRepository?
    val resourceRepository: ResourceRepository?
    val enquiryRepository: EnquiryRepository?
    val feedbackRepository: FeedbackRepository?
    val sessionManager: SessionManager?

    init {
        val url = normalizeUrl(BuildConfig.SUPABASE_URL)
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()

        configurationError = when {
            url.isBlank() || key.isBlank() ->
                "Supabase is not configured yet. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then rebuild the app."

            !url.startsWith("https://") && !url.startsWith("http://") ->
                "SUPABASE_URL looks invalid. It should start with https:// and end with .supabase.co."

            key.startsWith("sb_secret_") ->
                "A secret Supabase key was found in local.properties. Use the publishable/anon key instead."

            else -> null
        }

        if (configurationError == null) {
            val client = SupabaseProvider.createClient(url, key)
            val auth = SupabaseAuthRepository(client)
            val users = SupabaseUserRepository(client)
            val students = SupabaseStudentRepository(client)
            val appointments = SupabaseAppointmentRepository(client)
            val counsellors = SupabaseCounsellorRepository(client)
            val records = SupabaseSessionRecordRepository(client, appointments)
            val resources = SupabaseResourceRepository(client)
            val enquiries = SupabaseEnquiryRepository(client)
            val feedback = SupabaseFeedbackRepository(client)

            supabase = client
            authRepository = auth
            userRepository = users
            studentRepository = students
            appointmentRepository = appointments
            counsellorRepository = counsellors
            recordRepository = records
            resourceRepository = resources
            enquiryRepository = enquiries
            feedbackRepository = feedback
            sessionManager = SessionManager(auth, users)
        } else {
            supabase = null
            authRepository = null
            userRepository = null
            studentRepository = null
            appointmentRepository = null
            counsellorRepository = null
            recordRepository = null
            resourceRepository = null
            enquiryRepository = null
            feedbackRepository = null
            sessionManager = null
        }
    }

    private fun normalizeUrl(raw: String): String {
        var value = raw.trim()
        while (value.endsWith("/")) value = value.dropLast(1)
        if (value.endsWith("/rest/v1")) value = value.removeSuffix("/rest/v1")
        return value
    }
}
