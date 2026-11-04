// Module 3 | Owner: Jeremy
// Admin counsellor directory + counsellor/admin availability CRUD. RLS enforces
// who may insert, update or delete a slot; the database trigger enforces
// overlaps, past dates and booked-slot protection.
package com.example.counselling.feature.counsellor.data

import com.example.counselling.core.DateTimeUtils
import com.example.counselling.core.ErrorMapper
import com.example.counselling.feature.counsellor.model.AppointmentSlotUsageRow
import com.example.counselling.feature.counsellor.model.AvailabilitySlot
import com.example.counselling.feature.counsellor.model.AvailabilitySlotRow
import com.example.counselling.feature.counsellor.model.Counsellor
import com.example.counselling.feature.counsellor.model.CounsellorInsert
import com.example.counselling.feature.counsellor.model.CounsellorRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class AvailabilitySlotInsert(
    @SerialName("counsellor_id") val counsellorId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
)

class CounsellorRepository(private val supabase: SupabaseClient) {

    suspend fun getCounsellors(): Result<List<Counsellor>> = try {
        val rows = supabase
            .from("counsellors")
            .select { order("full_name", Order.ASCENDING) }
            .decodeList<CounsellorRow>()

        val freeSlots = supabase
            .from("available_slots_view")
            .select()
            .decodeList<com.example.counselling.feature.appointment.model.AvailableSlotRow>()

        val thisWeekIds = freeSlots
            .filter { DateTimeUtils.isWithinNextDays(it.startAt, 7) }
            .map { it.counsellorId }
            .toSet()

        Result.success(
            rows.map { it.toCounsellor(availableThisWeek = it.id in thisWeekIds) },
        )
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load counsellors.")))
    }

    suspend fun getCounsellor(counsellorId: String): Result<Counsellor?> = try {
        val row = supabase
            .from("counsellors")
            .select {
                filter { eq("id", counsellorId) }
                limit(1)
            }
            .decodeSingleOrNull<CounsellorRow>()

        if (row == null) {
            Result.success(null)
        } else {
            val slots = getSlotsInternal(counsellorId)
            Result.success(
                row.toCounsellor(
                    availableThisWeek = slots.any {
                        it.isEditable && DateTimeUtils.isWithinNextDays(it.startAt, 7)
                    },
                    slots = slots,
                ),
            )
        }
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load the counsellor.")))
    }

    suspend fun getMyCounsellor(userId: String): Result<Counsellor?> = try {
        val row = supabase
            .from("counsellors")
            .select {
                filter { eq("user_id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<CounsellorRow>()
        Result.success(row?.let { it.toCounsellor() })
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not find your counsellor profile.")))
    }

    suspend fun createCounsellor(
        fullName: String,
        email: String,
        specialisations: List<String>,
        languages: List<String>,
        bio: String,
    ): Result<Counsellor> = try {
        val row = supabase
            .from("counsellors")
            .insert(
                CounsellorInsert(
                    fullName = fullName.trim(),
                    email = email.trim().lowercase(),
                    specialisations = specialisations,
                    languages = languages,
                    bio = bio.trim(),
                ),
            ) {
                select()
            }
            .decodeSingle<CounsellorRow>()
        Result.success(row.toCounsellor())
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not add the counsellor.")))
    }

    suspend fun updateCounsellor(
        counsellorId: String,
        fullName: String,
        email: String,
        specialisations: List<String>,
        languages: List<String>,
        bio: String,
    ): Result<Counsellor> = try {
        val row = supabase
            .from("counsellors")
            .update(
                {
                    set("full_name", fullName.trim())
                    set("email", email.trim().lowercase())
                    set("specialisations", specialisations)
                    set("languages", languages)
                    set("bio", bio.trim())
                },
            ) {
                select()
                filter { eq("id", counsellorId) }
            }
            .decodeSingle<CounsellorRow>()
        Result.success(row.toCounsellor())
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the counsellor.")))
    }

    /** Returns "deleted" or "deactivated". */
    suspend fun removeCounsellor(counsellorId: String): Result<String> = try {
        val result = supabase.postgrest.rpc(
            "delete_counsellor",
            buildJsonObject { put("p_counsellor_id", counsellorId) },
        )
        Result.success(runCatching { result.decodeAs<String>() }.getOrDefault("deleted"))
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not remove the counsellor.")))
    }

    suspend fun getAvailability(counsellorId: String): Result<List<AvailabilitySlot>> = try {
        Result.success(getSlotsInternal(counsellorId))
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not load availability.")))
    }

    suspend fun createSlots(
        counsellorId: String,
        slots: List<Pair<String, String>>,
    ): Result<Unit> = try {
        val inserts = slots.map { (start, end) ->
            AvailabilitySlotInsert(counsellorId, start, end)
        }
        supabase.from("availability_slots").insert(inserts)
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not add availability.")))
    }

    suspend fun updateSlot(
        slotId: String,
        startAt: String,
        endAt: String,
    ): Result<Unit> = try {
        supabase.from("availability_slots").update(
            {
                set("start_at", startAt)
                set("end_at", endAt)
            },
        ) {
            filter { eq("id", slotId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not update the slot.")))
    }

    suspend fun deleteSlot(slotId: String): Result<Unit> = try {
        supabase.from("availability_slots").delete {
            filter { eq("id", slotId) }
        }
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(Exception(ErrorMapper.message(error, "Could not delete the slot.")))
    }

    private suspend fun getSlotsInternal(counsellorId: String): List<AvailabilitySlot> {
        val rows = supabase
            .from("availability_slots")
            .select {
                filter { eq("counsellor_id", counsellorId) }
                order("start_at", Order.ASCENDING)
            }
            .decodeList<AvailabilitySlotRow>()

        val usedSlotIds = supabase
            .from("appointments")
            .select {
                filter { eq("counsellor_id", counsellorId) }
            }
            .decodeList<AppointmentSlotUsageRow>()
            .map { it.slotId }
            .toSet()

        return rows.map {
            AvailabilitySlot(
                id = it.id,
                counsellorId = it.counsellorId,
                startAt = it.startAt,
                endAt = it.endAt,
                isBooked = it.id in usedSlotIds,
            )
        }
    }
}

private fun CounsellorRow.toCounsellor(
    availableThisWeek: Boolean = false,
    slots: List<AvailabilitySlot> = emptyList(),
) = Counsellor(
    id = id,
    userId = userId,
    fullName = fullName,
    email = email,
    specialisations = specialisations,
    languages = languages,
    bio = bio,
    isActive = isActive,
    availableThisWeek = availableThisWeek,
    slots = slots,
)
