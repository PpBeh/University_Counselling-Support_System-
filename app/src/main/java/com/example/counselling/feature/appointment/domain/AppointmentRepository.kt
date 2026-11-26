// Module 2 | Owner: Beh
// Repository abstraction for appointment reads and the existing RPC methods.
// ViewModels depend on this interface, never on SupabaseClient.
package com.example.counselling.feature.appointment.domain

import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.SessionMode

// OOP: Abstraction - a Supabase implementation and a fake implementation can
// both satisfy this contract.
interface AppointmentRepository {

    suspend fun getAppointments(): Result<List<Appointment>>

    suspend fun getAvailableSlots(): Result<List<AvailableSlot>>

    suspend fun getCounsellorSlots(counsellorId: String): Result<List<AvailableSlot>>

    suspend fun getSmartMatches(concern: ConcernCategory): Result<List<CounsellorMatch>>

    suspend fun bookAppointment(
        slotId: String,
        mode: SessionMode,
        concern: ConcernCategory,
        note: String?,
    ): Result<Unit>

    suspend fun rescheduleAppointment(
        appointmentId: String,
        newSlotId: String,
    ): Result<Unit>

    suspend fun counsellorRescheduleAppointment(
        appointmentId: String,
        newSlotId: String,
    ): Result<Unit>

    suspend fun confirmFollowUp(appointmentId: String): Result<Unit>

    suspend fun cancelAppointment(appointmentId: String): Result<Unit>

    suspend fun updateStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
    ): Result<Unit>
}
