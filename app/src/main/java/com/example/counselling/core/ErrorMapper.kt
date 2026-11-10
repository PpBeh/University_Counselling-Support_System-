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

            ("duplicate key" in raw ||
                "unique constraint" in raw ||
                "student id" in raw) &&
                "resources_title_lower_uidx" !in raw &&
                "counsellors_email_lower_uidx" !in raw ->
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

            "resources_title_lower_uidx" in raw ||
                "unique constraint" in raw && "title" in raw ->
                "A resource with this title already exists."

            "resources_url_valid" in raw ||
                "content_url" in raw && "check" in raw ->
                "Use a valid http:// or https:// resource link."

            "valid reference code" in raw ->
                "Enter a valid reference code, for example ENQ-7K3P9Q."

            "no open enquiry was found" in raw ->
                "No open enquiry was found for that reference code."

            "only the reply, status and recommended resource" in raw ->
                "Only the counsellor reply, status and attached resource can be changed."

            "write a reply before marking the enquiry as answered" in raw ->
                "Write a reply before marking the enquiry as answered."

            "only active students can submit enquiries" in raw ->
                "Only active students can submit enquiries."

            "message must be between 10 and 1,000" in raw ->
                "Your message must be between 10 and 1,000 characters."

            "already rated this session" in raw ->
                "You have already rated this session. Remove your rating before rating again."

            "feedback can only be added after a completed session" in raw ->
                "Feedback can only be added after a completed session."

            "counsellors_email_lower_uidx" in raw ||
                "counsellor with this email" in raw ->
                "A counsellor with this email already exists."

            "confirm or decline a pending booking" in raw ->
                "That status change is not allowed for this booking."

            "only confirmed sessions can be rescheduled by the counsellor" in raw ->
                "Only confirmed sessions can be rescheduled by the counsellor."

            "session has already started and cannot be cancelled" in raw ->
                "This session has already started and cannot be cancelled from the app."

            "already started and cannot be rescheduled" in raw ->
                "This session has already started and cannot be rescheduled."

            "that booking time has already passed" in raw ->
                "That booking time has already passed."

            "has upcoming appointments" in raw ->
                "This counsellor has upcoming appointments. Resolve them before removing the counsellor."

            "that counsellor could not be found" in raw ->
                "That counsellor could not be found."

            "end time must be after" in raw ->
                "The end time must be after the start time."

            "availability cannot be created or moved into the past" in raw ->
                "Availability cannot be created in the past."

            "overlaps another slot" in raw ->
                "This time overlaps another availability slot for this counsellor."

            "linked to an appointment and can no longer be edited" in raw ->
                "This slot is linked to an appointment and can no longer be edited."

            "linked to an appointment and cannot be deleted" in raw ->
                "This slot is linked to an appointment and cannot be deleted."

            "inactive and cannot receive new availability" in raw ->
                "This counsellor is inactive and cannot receive new availability."

            "only be edited within 7 days" in raw ->
                "Session records can only be edited within 7 days of creation. Archiving is still allowed."

            "follow-up due date must be in the future" in raw ->
                "The follow-up due date must be in the future."

            "archived session records" in raw ->
                "Archived session records cannot be used to schedule follow-ups."

            "archive this session record before deleting" in raw ->
                "Archive the session record before deleting it permanently."

            "student already has 2 active" in raw ->
                "This student already has 2 active upcoming appointments."

            "own free slots" in raw ->
                "Choose one of your own free availability slots."

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
