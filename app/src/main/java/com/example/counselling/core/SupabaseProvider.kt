// Core | Owner: shared (Beh, Jeremy, Michael)
// Builds the one Supabase client used by the whole app. Only the publishable
// client key is used; the service_role/secret key never belongs in Android.
package com.example.counselling.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    fun createClient(url: String, publishableKey: String): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = publishableKey,
        ) {
            install(Auth) {
                alwaysAutoRefresh = true
            }
            install(Postgrest)
        }
}
