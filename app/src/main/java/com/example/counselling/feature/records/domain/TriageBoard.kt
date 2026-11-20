// Module 4 / Unique Feature 2 | Owner: Jeremy
package com.example.counselling.feature.records.domain

import java.time.Instant

// OOP: Strategy/business object - the triage rules live on the aggregate.
enum class TriageBucket(val label: String) {
    OVERDUE("Overdue"),
    DUE_SOON("Due soon"),
    LATER("Later"),
}

class TriageBoard(private val now: Instant = Instant.now()) {

    fun bucketOf(record: SessionRecord): TriageBucket {
        val followUp = record.followUp
        return when {
            followUp == null -> if (record.isHighRisk) TriageBucket.DUE_SOON else TriageBucket.LATER
            followUp.isOverdue(now) -> TriageBucket.OVERDUE
            followUp.isDueSoon(now) -> TriageBucket.DUE_SOON
            else -> TriageBucket.LATER
        }
    }

    fun group(records: List<SessionRecord>): Map<TriageBucket, List<SessionRecord>> {
        val filtered = records.filter {
            !it.isArchived && (it.isHighRisk || it.followUpNeeded)
        }
        return TriageBucket.entries.associateWith { bucket ->
            filtered.filter { bucketOf(it) == bucket }
                .sortedWith(compareBy { it.followUp?.dueAt ?: Instant.MAX })
        }
    }
}
