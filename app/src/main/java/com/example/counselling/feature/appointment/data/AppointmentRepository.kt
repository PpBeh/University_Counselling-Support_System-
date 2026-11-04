// Module 2 | Owner: Beh
// All appointment changes go through the tested Postgres functions so slot
// locking, overlap checks and the maximum-active rule are enforced server-side.
package com.example.counselling.feature.appointment.data

import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.appointment.model.Appointment
import com.example.counselling.feature.appointment.model.AppointmentRow
import com.example.counselling.feature.appointment.model.AvailabilitySlotRow
import com.example.counselling.feature.appointment.model.AvailableSlotRow
import com.example.counselling.feature.appointment.model.CounsellorMatch
import com.example.counselling.feature.appointment.model.CounsellorSummaryRow
import com.example.counselling.feature.appointment.model.buildSmartMatches
import com.example.counselling.feature.student.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppointmentRepository(private val supabase: SupabaseClient) {

    /**
     * RLS scopes this query: a student gets their own rows, a counsellor gets
     * their assigned rows, and an admin gets all rows. Related names are
     * assembled in the app with small follow-up reads to keep the query DSL
     * simple and readable.
     */
    suspend fun getAppointments(): Result<List<Appointment>> = try {
        val rows = supabase
            .from("appointments")
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<AppointmentRow>()

        val slots = supabase
            .from("availability_slots")
            .select()
            .decodeList<AvailabilitySlotRow>()
            .associateBy { it.id }

        val counsellors = supabase
            .from("counsellors")
            .select()
            .decodeList<CounsellorSummaryRow>()
            .associateBy { it.id }

        val students = supabase
            .from("profiles")
            .select {
                filter { eq("role", "student") }
            }
            .decodeList<Profile>()
            .associateBy { it.id }

        val appointments = rows.map { row ->
            val slot = slots[row.slotId]
            val counsellor = counsellors[row.counsellorId]
            val student = students[row.studentId]
            Appointment(
                id = row.id,
                studentId = row.studentId,
                counsellorId = row.counsellorId,
                slotId = row.slotId,
                mode = row.mode,
                concernCategory = row.concernCategory,
                note = row.note,
                status = row.status,
                followUpOf = row.followUpOf,
                createdAt = row.createdAt,
                slotStart = slot?.startAt.orEmpty(),
                slotEnd = slot?.endAt.orEmpty(),
                counsellorName = counsellor?.fullName ?: "Counsellor",
                counsellorSpecialisations = counsellor?.specialisations ?: emptyList(),
                studentName = student?.fullName ?: "Student",
                studentEmail = student?.email,
                studentCode = student?.studentId,
                studentPhone = student?.phone,
            )
        }
        Result.success(appointments)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load appointments.")))
    }

    suspend fun getAvailableSlots(): Result<List<AvailableSlotRow>> = try {
        val slots = supabase
            .from("available_slots_view")
            .select {
                order("start_at", Order.ASCENDING)
            }
            .decodeList<AvailableSlotRow>()
        Result.success(slots)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load available slots.")))
    }

    /**
     * Smart Counsellor Match: keep only counsellors whose specialisations
     * include the chosen concern, rank by earliest free slot and return the
     * next free slot for the card.
     */
    suspend fun getSmartMatches(concernCategory: String): Result<List<CounsellorMatch>> {
        val slotsResult = getAvailableSlots()
        return slotsResult.map { slots ->
            buildSmartMatches(slots, concernCategory)
        }.mapError("Could not match you with a counsellor.")
    }

    suspend fun bookAppointment(
        slotId: String,
        mode: String,
        concernCategory: String,
        note: String?,
    ): Result<Unit> = try {
        val params = buildJsonObject {
            put("p_slot_id", slotId)
            put("p_mode", mode)
            put("p_concern_category", concernCategory)
            if (!note.isNullOrBlank()) put("p_note", note.trim())
        }
        supabase.postgrest.rpc("book_appointment", params)
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not book the appointment.")))
    }

    suspend fun rescheduleAppointment(appointmentId: String, newSlotId: String): Result<Unit> = try {
        supabase.postgrest.rpc(
            "reschedule_appointment",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_new_slot_id", newSlotId)
            },
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not reschedule the appointment.")))
    }

    suspend fun cancelAppointment(appointmentId: String): Result<Unit> = try {
        supabase.postgrest.rpc(
            "cancel_appointment",
            buildJsonObject { put("p_appointment_id", appointmentId) },
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not cancel the appointment.")))
    }

    suspend fun updateStatus(appointmentId: String, newStatus: String): Result<Unit> = try {
        supabase.postgrest.rpc(
            "update_appointment_status",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_new_status", newStatus)
            },
        )
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the appointment status.")))
    }
}

private fun <T> Result<T>.mapError(fallback: String): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(Exception(it.message ?: fallback)) },
)
