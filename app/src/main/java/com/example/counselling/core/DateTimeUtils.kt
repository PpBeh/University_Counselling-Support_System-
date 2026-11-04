// Core | Owner: shared (Beh, Jeremy, Michael)
// Supabase stores timestamptz values in UTC. Everything the student sees is
// displayed in Asia/Kuala_Lumpur.
package com.example.counselling.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val kualaLumpur: ZoneId = ZoneId.of(AppConstants.TIME_ZONE)
    private val dateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    private val dateFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val timeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun nowInKualaLumpur(): ZonedDateTime = ZonedDateTime.now(kualaLumpur)

    fun greeting(): String = when (nowInKualaLumpur().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    fun todayHeading(): String =
        nowInKualaLumpur().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))

    fun parse(value: String?): ZonedDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            OffsetDateTime.parse(value).atZoneSameInstant(kualaLumpur)
        }.recoverCatching {
            ZonedDateTime.parse(value).withZoneSameInstant(kualaLumpur)
        }.recoverCatching {
            Instant.parse(value).atZone(kualaLumpur)
        }.getOrNull()
    }

    fun formatDateTime(value: String?): String =
        parse(value)?.format(dateTimeFormatter) ?: "—"

    fun formatDate(value: String?): String =
        parse(value)?.format(dateFormatter) ?: "—"

    fun formatTime(value: String?): String =
        parse(value)?.format(timeFormatter) ?: "—"

    /** Positive when the timestamp is still in the future. */
    fun hoursUntil(value: String?): Long {
        val target = parse(value) ?: return Long.MIN_VALUE
        return java.time.Duration.between(nowInKualaLumpur(), target).toHours()
    }

    fun canReschedule(value: String?): Boolean = hoursUntil(value) >= AppConstants.MIN_NOTICE_HOURS

    fun isFuture(value: String?): Boolean {
        val target = parse(value) ?: return false
        return target.isAfter(nowInKualaLumpur())
    }

    fun isWithinNextDays(value: String?, days: Long): Boolean {
        val target = parse(value) ?: return false
        val now = nowInKualaLumpur()
        return target.isAfter(now) && target.isBefore(now.plusDays(days))
    }

    fun isPast(value: String?): Boolean = parse(value)?.isBefore(nowInKualaLumpur()) ?: true

    /** Converts a date-picker millis value + KL wall-clock time to a UTC API timestamp. */
    fun toApiTimestamp(datePickerMillis: Long, hour: Int, minute: Int): String {
        val date = dateFromPickerMillis(datePickerMillis)
        return date
            .atTime(hour, minute)
            .atZone(kualaLumpur)
            .toInstant()
            .toString()
    }

    fun toApiTimestamp(date: LocalDate, hour: Int, minute: Int): String =
        date.atTime(hour, minute).atZone(kualaLumpur).toInstant().toString()

    fun localDate(value: String?): LocalDate? = parse(value)?.toLocalDate()

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null) return "Choose a date"
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return date.format(dateFormatter)
    }

    fun formatTime(hour: Int, minute: Int): String =
        LocalTime.of(hour, minute).format(timeFormatter)

    fun datePickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun dateFromPickerMillis(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
