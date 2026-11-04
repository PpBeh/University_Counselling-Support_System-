// Module 3 | Owner: Jeremy
// Pure repeat-week generation, kept testable and separate from Compose.
package com.example.counselling.feature.counsellor.model

import java.time.Instant
import java.time.temporal.ChronoUnit

internal fun buildWeeklySlots(
    start: Instant,
    end: Instant,
    repeatWeeks: Int,
): List<Pair<Instant, Instant>> {
    require(repeatWeeks in 1..8) { "Repeat must be between 1 and 8 weeks." }
    require(end.isAfter(start)) { "End time must be after start time." }
    return (0 until repeatWeeks).map { week ->
        val offset = week.toLong() * 7
        start.plus(offset, ChronoUnit.DAYS) to end.plus(offset, ChronoUnit.DAYS)
    }
}
