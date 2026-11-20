// Module 2 | Owner: Beh
package com.example.counselling.feature.appointment.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.domain.AppointmentStatus
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.PhoneNumber
import com.example.counselling.core.domain.SessionMode
import com.example.counselling.core.domain.Student
import com.example.counselling.core.domain.StudentId
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.feature.appointment.domain.Appointment
import com.example.counselling.feature.appointment.domain.AppointmentRepository
import com.example.counselling.feature.appointment.domain.AvailableSlot
import com.example.counselling.feature.appointment.domain.CounsellorMatch
import com.example.counselling.feature.appointment.domain.CounsellorMatchStrategy
import com.example.counselling.feature.appointment.domain.SpecialisationThenEarliestSlotStrategy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class AppointmentViewRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("student_phone") val studentPhone: String? = null,
    @SerialName("student_is_active") val studentIsActive: Boolean = true,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("student_programme") val studentProgramme: String? = null,
    @SerialName("student_preferred_mode") val studentPreferredMode: String? = null,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("counsellor_name") val counsellorName: String,
    @SerialName("counsellor_email") val counsellorEmail: String? = null,
    @SerialName("counsellor_phone") val counsellorPhone: String? = null,
    @SerialName("counsellor_is_active") val counsellorIsActive: Boolean = true,
    @SerialName("counsellor_bio") val counsellorBio: String = "",
    @SerialName("counsellor_specialisations") val counsellorSpecialisations: List<String> = emptyList(),
    @SerialName("counsellor_languages") val counsellorLanguages: List<String> = emptyList(),
    @SerialName("slot_id") val slotId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("mode_code") val modeCode: String,
    @SerialName("mode_label") val modeLabel: String? = null,
    @SerialName("concern_code") val concernCode: String,
    @SerialName("concern_category") val concernCategory: String? = null,
    val note: String? = null,
    @SerialName("status_code") val statusCode: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("is_terminal") val isTerminal: Boolean? = null,
    @SerialName("follow_up_of") val followUpOf: String? = null,
    @SerialName("last_event_code") val lastEvent: String = "booked",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class AvailableSlotViewRow(
    @SerialName("slot_id") val slotId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("counsellor_name") val counsellorName: String,
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
)

// OOP: Repository implementation - returns domain Appointments/AvailableSlots,
// never the DTO rows above.
class SupabaseAppointmentRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
    private val matchStrategy: CounsellorMatchStrategy = SpecialisationThenEarliestSlotStrategy(),
) : BaseSupabaseRepository(supabase, errorTranslator), AppointmentRepository {

    override suspend fun getAppointments(): Result<List<Appointment>> =
        runQuery("Could not load appointments.") {
            supabase
                .from("v_appointments")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<AppointmentViewRow>()
                .map { it.toDomain() }
        }

    override suspend fun getAvailableSlots(): Result<List<AvailableSlot>> =
        runQuery("Could not load available slots.") {
            supabase
                .from("v_availability_slots")
                .select { order("start_at", Order.ASCENDING) }
                .decodeList<AvailableSlotViewRow>()
                .map { it.toDomain() }
        }

    override suspend fun getSmartMatches(
        concern: ConcernCategory,
    ): Result<List<CounsellorMatch>> =
        getAvailableSlots().map { slots -> matchStrategy.match(slots, concern) }

    override suspend fun bookAppointment(
        slotId: String,
        mode: SessionMode,
        concern: ConcernCategory,
        note: String?,
    ): Result<Unit> = runQuery("Could not book the appointment.") {
        val params = buildJsonObject {
            put("p_slot_id", slotId)
            put("p_mode", mode.code)
            put("p_concern_category", concern.label)
            if (!note.isNullOrBlank()) put("p_note", note.trim())
        }
        supabase.postgrest.rpc("book_appointment", params)
        Unit
    }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newSlotId: String,
    ): Result<Unit> = runQuery("Could not reschedule the appointment.") {
        supabase.postgrest.rpc(
            "reschedule_appointment",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_new_slot_id", newSlotId)
            },
        )
        Unit
    }

    override suspend fun counsellorRescheduleAppointment(
        appointmentId: String,
        newSlotId: String,
    ): Result<Unit> = runQuery("Could not reschedule the session.") {
        supabase.postgrest.rpc(
            "counsellor_reschedule_appointment",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_new_slot_id", newSlotId)
            },
        )
        Unit
    }

    override suspend fun confirmFollowUp(appointmentId: String): Result<Unit> =
        runQuery("Could not confirm the follow-up.") {
            supabase.postgrest.rpc(
                "confirm_followup_appointment",
                buildJsonObject { put("p_appointment_id", appointmentId) },
            )
            Unit
        }

    override suspend fun cancelAppointment(appointmentId: String): Result<Unit> =
        runQuery("Could not cancel the appointment.") {
            supabase.postgrest.rpc(
                "cancel_appointment",
                buildJsonObject { put("p_appointment_id", appointmentId) },
            )
            Unit
        }

    override suspend fun updateStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
    ): Result<Unit> = runQuery("Could not update the appointment status.") {
        supabase.postgrest.rpc(
            "update_appointment_status",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_new_status", newStatus.wireValue)
            },
        )
        Unit
    }

    private fun AppointmentViewRow.toDomain(): Appointment {
        val start = parseInstant(startAt)
        val end = parseInstant(endAt)
        val slot = TimeSlot.fromStored(start, end)

        val student = Student(
            id = studentId,
            email = Email.of(studentEmail ?: throw DomainException("Student email is missing.")),
            fullName = studentName,
            phone = studentPhone?.takeIf { it.isNotBlank() }?.let(PhoneNumber::of),
            isActive = studentIsActive,
            studentId = StudentId.of(studentCode ?: throw DomainException("Student ID is missing.")),
            programme = studentProgramme ?: throw DomainException("Student programme is missing."),
            preferredMode = SessionMode.fromWire(studentPreferredMode)
                ?: throw DomainException("Student preferred mode is missing."),
        )

        val specialisations = counsellorSpecialisations.map { value ->
            ConcernCategory.fromWire(value)
                ?: throw DomainException("Unknown counsellor specialisation: $value")
        }
        val counsellor = Counsellor(
            id = counsellorId,
            email = Email.of(counsellorEmail ?: throw DomainException("Counsellor email is missing.")),
            fullName = counsellorName,
            phone = counsellorPhone?.takeIf { it.isNotBlank() }?.let(PhoneNumber::of),
            isActive = counsellorIsActive,
            specialisations = specialisations,
            languages = counsellorLanguages,
            bio = counsellorBio,
        )

        return Appointment(
            id = id,
            student = student,
            counsellor = counsellor,
            slotId = slotId,
            slot = slot,
            mode = SessionMode.fromWire(modeCode)
                ?: throw DomainException("Unknown session mode: ${modeCode}"),
            concern = ConcernCategory.fromWire(concernCode)
                ?: throw DomainException("Unknown concern: ${concernCode}"),
            note = note,
            status = AppointmentStatus.fromWire(statusCode),
            followUpOf = followUpOf,
            createdAt = createdAt?.let(::parseInstant),
            lastEvent = lastEvent,
            updatedAt = updatedAt?.let(::parseInstant),
        )
    }

    private fun AvailableSlotViewRow.toDomain(): AvailableSlot = AvailableSlot(
        slotId = slotId,
        counsellorId = counsellorId,
        counsellorName = counsellorName,
        specialisations = specialisations.map { value ->
            ConcernCategory.fromWire(value)
                ?: throw DomainException("Unknown counsellor specialisation: $value")
        },
        languages = languages,
        bio = bio,
        timeSlot = TimeSlot.fromStored(
            parseInstant(startAt),
            parseInstant(endAt),
        ),
    )

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
