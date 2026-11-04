// Core | Owner: shared (Beh, Jeremy, Michael)
// Never show raw Supabase/Postgrest exceptions to a student. This mapper
// turns the common cases into short, friendly messages.
package com.example.counselling.core

import java.util.Locale

object ErrorMapper {

    fun message(error: Throwable, fallback: String = "Something went wrong. Please try again."): String {
        val raw = error.message.orEmpty().lowercase(Locale.ROOT)

        return when {
            "invalid login credentials" in raw ->
                "Email or password is incorrect."

            "email not confirmed" in raw ->
                "Please confirm your email before signing in."

            "user already registered" in raw || "already been registered" in raw ->
                "An account with this email already exists."

            "weak" in raw && "password" in raw ->
                "Please choose a stronger password."

            "database error saving new user" in raw ->
                "Registration could not be completed. Check your details; the email or student ID may already be registered."

            "duplicate key" in raw ||
                "unique constraint" in raw ||
                "student id" in raw ->
                "That student ID or email is already registered."

            "failed to fetch" in raw ||
                "timeout" in raw ||
                "timed out" in raw ||
                "connect" in raw ||
                "network" in raw ||
                "unable to resolve host" in raw ->
                "Cannot reach the server. Check your internet connection and try again."

            "permission denied" in raw ||
                "row-level security" in raw ||
                "42501" in raw ->
                "You do not have permission to do that."

            "at most 2 active upcoming" in raw ->
                "You can have at most 2 active upcoming appointments. Cancel one before booking another."

            "overlaps this time" in raw ||
                "overlapping" in raw ->
                "You already have another appointment at that time."

            "has already passed" in raw ||
                "no longer available" in raw ->
                "That time slot is no longer available. Please choose another."

            "just booked" in raw ||
                "already booked" in raw ->
                "That time slot was just booked. Please choose another slot."

            "12 hours" in raw ->
                "Appointments can only be rescheduled at least 12 hours in advance."

            "already started" in raw ->
                "This appointment has already started. Please contact the counselling office."

            "still have upcoming" in raw ->
                "Cancel your upcoming appointments before deactivating your account."

            "account is inactive" in raw ||
                "account has been deactivated" in raw ->
                "This account is inactive. Please contact the counselling office."

            "record already exists" in raw ||
                "already exists for this appointment" in raw ->
                "A record already exists for that appointment."

            "can only be added for completed" in raw ->
                "Session records can only be added for completed appointments."

            "cannot be changed" in raw ||
                "status change is not allowed" in raw ->
                "That change is not allowed for this appointment."

            "not found in your schedule" in raw ||
                "could not be found" in raw ->
                "That appointment could not be found. Refresh and try again."

            "jwt" in raw || "not authenticated" in raw || "401" in raw ->
                "Your session has expired. Please sign in again."

            "relation" in raw && "does not exist" in raw ->
                "The App database is not ready. Please contact the administrator."

            "profile" in raw && "could not be found" in raw ->
                "We could not load your profile. Please sign in again."

            else -> fallback
        }
    }
}
