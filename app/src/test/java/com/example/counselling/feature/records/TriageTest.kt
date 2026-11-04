// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records

import com.example.counselling.core.DateTimeUtils
import com.example.counselling.feature.records.model.SessionRecord
import com.example.counselling.feature.records.model.TriageBucket
import com.example.counselling.feature.records.model.triageRecords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriageTest {

    private fun record(
        id: String,
        risk: String,
        followUp: Boolean,
        due: String?,
        archived: Boolean = false,
    ) = SessionRecord(
        id = id,
        appointmentId = "appointment-$id",
        counsellorId = "counsellor",
        studentId = "student",
        privateNotes = "Private notes for $id are long enough.",
        publicSummary = "Summary $id",
        riskLevel = risk,
        followUpNeeded = followUp,
        followUpDue = due,
        isArchived = archived,
        createdAt = DateTimeUtils.nowInKualaLumpur().toString(),
        updatedAt = null,
        appointment = null,
    )

    @Test
    fun triage_groupsOverdueSoonLaterAndExcludesIrrelevant() {
        val now = DateTimeUtils.nowInKualaLumpur()
        val records = listOf(
            record("overdue", "low", true, now.minusDays(2).toString()),
            record("soon", "high", false, null),
            record("later", "medium", true, now.plusDays(10).toString()),
            record("no-follow-up", "low", false, null),
            record("archived", "high", true, now.minusDays(1).toString(), archived = true),
        )

        val triage = records.triageRecords()

        assertEquals(listOf("overdue"), triage[TriageBucket.OVERDUE]?.map { it.id })
        assertEquals(listOf("soon"), triage[TriageBucket.DUE_SOON]?.map { it.id })
        assertEquals(listOf("later"), triage[TriageBucket.LATER]?.map { it.id })
        assertTrue(triage.values.flatten().none { it.id == "no-follow-up" })
        assertTrue(triage.values.flatten().none { it.id == "archived" })
    }

    @Test
    fun dueSoon_includesFollowUpWithinSevenDays() {
        val now = DateTimeUtils.nowInKualaLumpur()
        val records = listOf(
            record("three-days", "low", true, now.plusDays(3).toString()),
            record("eight-days", "low", true, now.plusDays(8).toString()),
        )

        val triage = records.triageRecords()

        assertEquals(listOf("three-days"), triage[TriageBucket.DUE_SOON]?.map { it.id })
        assertEquals(listOf("eight-days"), triage[TriageBucket.LATER]?.map { it.id })
    }
}
