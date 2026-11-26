// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.data

import com.example.counselling.core.data.BaseSupabaseRepository
import com.example.counselling.core.data.ErrorTranslator
import com.example.counselling.core.data.UserViewRow
import com.example.counselling.core.domain.AvailabilitySlot
import com.example.counselling.core.domain.ConcernCategory
import com.example.counselling.core.domain.Counsellor
import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.Email
import com.example.counselling.core.domain.TimeSlot
import com.example.counselling.core.domain.UserFactory
import com.example.counselling.feature.counsellor.domain.CounsellorRemovalResult
import com.example.counselling.feature.counsellor.domain.CounsellorRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class AvailabilityTableRow(
    val id: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class AvailableSlotViewRow(
    @SerialName("slot_id") val slotId: String,
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("counsellor_name") val counsellorName: String = "",
    val specialisations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bio: String = "",
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("is_booked") val isBooked: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class SlotUsageRow(
    @SerialName("slot_id") val slotId: String,
)

@Serializable
private data class AvailabilitySlotInsert(
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

// OOP: Repository implementation - counsellor aggregate and availability CRUD.
class SupabaseCounsellorRepository(
    supabase: SupabaseClient,
    errorTranslator: ErrorTranslator = ErrorTranslator(),
) : BaseSupabaseRepository(supabase, errorTranslator), CounsellorRepository {

    override suspend fun getCounsellors(): Result<List<Counsellor>> =
        runQuery("Could not load counsellors.") {
            val counsellors = supabase
                .from("v_counsellors")
                .select { order("full_name", Order.ASCENDING) }
                .decodeList<UserViewRow>()
                .map(::toCounsellor)

            val freeByCounsellor = supabase
                .from("v_availability_slots")
                .select { order("start_at", Order.ASCENDING) }
                .decodeList<AvailableSlotViewRow>()
                .map(::toDomain)
                .filterNot { it.isBooked }
                .groupBy { it.counsellorId }

            counsellors.map { it.withAvailability(freeByCounsellor[it.id].orEmpty()) }
        }

    override suspend fun getCounsellor(counsellorId: String): Result<Counsellor?> =
        runQuery("Could not load the counsellor.") {
            val row = supabase
                .from("v_counsellors")
                .select {
                    filter { eq("id", counsellorId) }
                    limit(1)
                }
                .decodeSingleOrNull<UserViewRow>()
                ?: return@runQuery null

            val slots = loadAvailability(counsellorId)
            toCounsellor(row).withAvailability(slots)
        }

    override suspend fun getMyCounsellor(authUserId: String): Result<Counsellor?> =
        runQuery("Could not find your counsellor profile.") {
            val row = supabase
                .from("v_counsellors")
                .select {
                    filter { eq("auth_user_id", authUserId) }
                    limit(1)
                }
                .decodeSingleOrNull<UserViewRow>()
                ?: return@runQuery null
            toCounsellor(row).withAvailability(loadAvailability(row.id))
        }

    override suspend fun createCounsellor(
        fullName: String,
        email: Email,
        specialisations: List<ConcernCategory>,
        languages: List<String>,
        bio: String,
    ): Result<Counsellor> = runQuery("Could not add the counsellor.") {
        val id = supabase.postgrest.rpc(
            "create_counsellor",
            buildJsonObject {
                put("p_full_name", fullName.trim())
                put("p_email", email.value)
                put("p_specialisations", buildJsonArray { specialisations.forEach { add(it.label) } })
                put("p_languages", buildJsonArray { languages.forEach { add(it) } })
                put("p_bio", bio.trim())
            },
        ).decodeAs<String>()

        getCounsellor(id).getOrElse { throw it }
            ?: throw DomainException("The new counsellor could not be reloaded.")
    }

    override suspend fun updateCounsellor(
        counsellorId: String,
        fullName: String,
        email: Email,
        specialisations: List<ConcernCategory>,
        languages: List<String>,
        bio: String,
        isActive: Boolean,
    ): Result<Counsellor> = runQuery("Could not save the counsellor.") {
        supabase.postgrest.rpc(
            "update_counsellor",
            buildJsonObject {
                put("p_counsellor_id", counsellorId)
                put("p_full_name", fullName.trim())
                put("p_email", email.value)
                put("p_specialisations", buildJsonArray { specialisations.forEach { add(it.label) } })
                put("p_languages", buildJsonArray { languages.forEach { add(it) } })
                put("p_bio", bio.trim())
                put("p_is_active", isActive)
            },
        )
        getCounsellor(counsellorId).getOrElse { throw it }
            ?: throw DomainException("The updated counsellor could not be reloaded.")
    }

    override suspend fun removeCounsellor(
        counsellorId: String,
    ): Result<CounsellorRemovalResult> = runQuery("Could not remove the counsellor.") {
        val outcome = supabase.postgrest.rpc(
            "delete_counsellor",
            buildJsonObject { put("p_counsellor_id", counsellorId) },
        ).decodeAs<String>()

        if (outcome == "deleted") {
            CounsellorRemovalResult.Deleted
        } else {
            CounsellorRemovalResult.Deactivated
        }
    }

    override suspend fun getAvailability(
        counsellorId: String,
    ): Result<List<AvailabilitySlot>> =
        runQuery("Could not load availability.") { loadAvailability(counsellorId) }

    override suspend fun createSlots(
        counsellorId: String,
        slots: List<TimeSlot>,
    ): Result<Unit> = runQuery("Could not save availability.") {
        if (slots.isEmpty()) return@runQuery Unit
        supabase.from("availability_slots").insert(
            slots.map {
                AvailabilitySlotInsert(
                    counsellorId = counsellorId,
                    startAt = it.start.toString(),
                    endAt = it.end.toString(),
                )
            },
        )
        Unit
    }

    override suspend fun updateSlot(
        slotId: String,
        slot: TimeSlot,
    ): Result<Unit> = runQuery("Could not update the slot.") {
        supabase.from("availability_slots").update(
            {
                set("start_at", slot.start.toString())
                set("end_at", slot.end.toString())
            },
        ) {
            filter { eq("id", slotId) }
        }
        Unit
    }

    override suspend fun deleteSlot(slotId: String): Result<Unit> =
        runQuery("Could not delete the slot.") {
            supabase.from("availability_slots").delete {
                filter { eq("id", slotId) }
            }
            Unit
        }

    private suspend fun loadAvailability(counsellorId: String): List<AvailabilitySlot> {
        val rows = supabase
            .from("availability_slots")
            .select {
                filter { eq("counsellor_id", counsellorId) }
                order("start_at", Order.ASCENDING)
            }
            .decodeList<AvailabilityTableRow>()

        val booked = supabase
            .from("appointments")
            .select { filter { eq("counsellor_id", counsellorId) } }
            .decodeList<SlotUsageRow>()
            .map { it.slotId }
            .toSet()

        return rows.map {
            AvailabilitySlot(
                id = it.id,
                counsellorId = it.counsellorId,
                timeSlot = TimeSlot.fromStored(parseInstant(it.startAt), parseInstant(it.endAt)),
                isBooked = it.id in booked,
            )
        }
    }

    private fun toCounsellor(row: UserViewRow): Counsellor {
        val user = UserFactory.fromRecord(row.toRecord())
        return user as? Counsellor
            ?: throw DomainException("That user is not a counsellor.")
    }

    private fun toDomain(row: AvailableSlotViewRow): AvailabilitySlot = AvailabilitySlot(
        id = row.slotId,
        counsellorId = row.counsellorId,
        timeSlot = TimeSlot.fromStored(parseInstant(row.startAt), parseInstant(row.endAt)),
        isBooked = row.isBooked,
    )

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { ZonedDateTime.parse(value).toInstant() }
            .getOrElse { throw DomainException("A date value could not be read: $value") }
}
