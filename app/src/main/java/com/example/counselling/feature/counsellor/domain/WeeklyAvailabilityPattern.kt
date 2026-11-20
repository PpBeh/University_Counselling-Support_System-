// Module 3 | Owner: Jeremy
package com.example.counselling.feature.counsellor.domain

import com.example.counselling.core.domain.DomainException
import com.example.counselling.core.domain.TimeSlot
import java.time.Duration

// OOP: Value object - a weekly repeat pattern owns the repeat rule and emits
// complete TimeSlot objects; the UI no longer builds date strings by hand.
class WeeklyAvailabilityPattern(
    private val firstSlot: TimeSlot,
    private val repeatWeeks: Int,
) {
    init {
        if (repeatWeeks !in 1..8) {
            throw DomainException("Choose between 1 and 8 weeks.")
        }
    }

    fun generate(): List<TimeSlot> = (0 until repeatWeeks).map { week ->
        val start = firstSlot.start.plus(Duration.ofDays((week * 7).toLong()))
        val end = firstSlot.end.plus(Duration.ofDays((week * 7).toLong()))
        TimeSlot.fromStored(start, end)
    }
}
