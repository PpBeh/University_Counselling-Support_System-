// Core | Owner: shared (Beh, Jeremy, Michael)
// Simple manual dependency container. One instance is created by
// CounsellingApp; ViewModels receive their repositories through a factory.
package com.example.counselling.core

import com.example.counselling.BuildConfig
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.auth.data.AuthRepository
import com.example.counselling.feature.student.data.ProfileRepository
import io.github.jan.supabase.SupabaseClient

class AppContainer {

    val configurationError: String?

    val supabase: SupabaseClient?
    val authRepository: AuthRepository?
    val profileRepository: ProfileRepository?
    val appointmentRepository: AppointmentRepository?
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
            val auth = AuthRepository(client)
            val profiles = ProfileRepository(client)
            val appointments = AppointmentRepository(client)
            supabase = client
            authRepository = auth
            profileRepository = profiles
            appointmentRepository = appointments
            sessionManager = SessionManager(auth, profiles)
        } else {
            supabase = null
            authRepository = null
            profileRepository = null
            appointmentRepository = null
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
