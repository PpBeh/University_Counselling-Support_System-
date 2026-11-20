// Core domain | Owner: shared (Beh, Jeremy, Michael)
// Pure-Kotlin value objects. They do not import Android or Supabase.
package com.example.counselling.core.domain

import java.time.Duration
import java.time.Instant

// OOP: Encapsulation - Email validates and normalises itself. An invalid
// Email object cannot be constructed through the public API.
class Email private constructor(val value: String) {

    val domain: String
        get() = value.substringAfter('@')

    override fun equals(other: Any?): Boolean =
        this === other || (other is Email && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        private val emailRegex =
            Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

        fun of(raw: String): Email {
            val text = raw.trim()
            if (text.isEmpty()) {
                throw DomainException("Email is required.")
            }
            if (text.length > 254 || !emailRegex.matches(text)) {
                throw DomainException("Enter a valid email address.")
            }
            return Email(text.lowercase())
        }
    }
}

// OOP: Encapsulation - Malaysian mobile number format is enforced by the type.
class PhoneNumber private constructor(val value: String) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is PhoneNumber && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        private val phoneRegex = Regex("""^(01[0-9]-[0-9]{7}|\+601[0-9][0-9]{7,8})$""")

        fun of(raw: String): PhoneNumber {
            val text = raw.trim()
            if (text.isEmpty()) {
                throw DomainException("Phone number is required.")
            }
            if (!phoneRegex.matches(text)) {
                throw DomainException(
                    "Use a Malaysian number such as 012-3456789 or +60123456789.",
                )
            }
            return PhoneNumber(text)
        }
    }
}

// OOP: Encapsulation - Password keeps its characters private, validates the
// strength rules once, and never exposes them through toString().
class Password private constructor(private val raw: String) {

    val length: Int
        get() = raw.length

    fun matches(candidate: String): Boolean = raw == candidate

    // OOP: Encapsulation - only the auth data layer may reveal the value.
    internal fun reveal(): String = raw

    override fun toString(): String = "Password(***)"

    companion object {
        fun of(raw: String): Password {
            if (raw.isEmpty()) {
                throw DomainException("Password is required.")
            }
            if (raw.length < 8) {
                throw DomainException("Password must be at least 8 characters.")
            }
            if (!raw.any { it.isUpperCase() }) {
                throw DomainException("Password needs at least one uppercase letter.")
            }
            if (!raw.any { it.isLowerCase() }) {
                throw DomainException("Password needs at least one lowercase letter.")
            }
            if (!raw.any { it.isDigit() }) {
                throw DomainException("Password needs at least one number.")
            }
            return Password(raw)
        }
    }
}

// OOP: Encapsulation - a student number is a domain value, not an arbitrary
// String passed around with separate validation.
class StudentId private constructor(val value: String) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is StudentId && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        fun of(raw: String): StudentId {
            val text = raw.trim()
            if (text.isEmpty()) {
                throw DomainException("Student ID is required.")
            }
            if (text.length < 3) {
                throw DomainException("Student ID is too short.")
            }
            if (text.length > 30) {
                throw DomainException("Student ID must be 30 characters or fewer.")
            }
            return StudentId(text)
        }
    }
}

// OOP: Encapsulation - a booking interval validates its order. New bookings
// use of()/future(); historical rows use fromStored() so old sessions remain
// representable without weakening the future-only command rule.
class TimeSlot private constructor(
    val start: Instant,
    val end: Instant,
) {

    init {
        if (!start.isBefore(end)) {
            throw DomainException("The end time must be after the start time.")
        }
    }

    fun durationMinutes(): Long = Duration.between(start, end).toMinutes()

    fun overlaps(other: TimeSlot): Boolean =
        start.isBefore(other.end) && end.isAfter(other.start)

    fun isFuture(now: Instant = Instant.now()): Boolean = start.isAfter(now)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is TimeSlot && start == other.start && end == other.end)

    override fun hashCode(): Int = 31 * start.hashCode() + end.hashCode()

    override fun toString(): String = "[$start, $end)"

    companion object {

        // OOP: Encapsulation - future-only factory for booking/availability.
        fun of(start: Instant, end: Instant, now: Instant = Instant.now()): TimeSlot {
            val slot = TimeSlot(start, end)
            if (!slot.start.isAfter(now)) {
                throw DomainException("That time slot has already passed. Please choose another.")
            }
            return slot
        }

        // OOP: Persistence boundary - historical data must still be readable.
        fun fromStored(start: Instant, end: Instant): TimeSlot = TimeSlot(start, end)
    }
}

// OOP: Encapsulation - Rating can only be 1..5.
class Rating private constructor(val value: Int) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is Rating && value == other.value)

    override fun hashCode(): Int = value

    override fun toString(): String = value.toString()

    companion object {
        fun of(value: Int): Rating {
            if (value !in 1..5) {
                throw DomainException("Rating must be between 1 and 5.")
            }
            return Rating(value)
        }
    }
}

// OOP: Encapsulation - enquiry reference codes are validated and normalised.
class ReferenceCode private constructor(val value: String) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is ReferenceCode && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        private val codeRegex = Regex("""^ENQ-[A-Z0-9]{6}$""")

        fun of(raw: String): ReferenceCode {
            val text = raw.trim().uppercase()
            if (!codeRegex.matches(text)) {
                throw DomainException(
                    "Please enter a valid reference code (for example ENQ-7K3P9Q).",
                )
            }
            return ReferenceCode(text)
        }
    }
}
