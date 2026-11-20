// Core domain | Owner: shared (Beh, Jeremy, Michael)
// Type-safe vocabulary objects that mirror the database reference tables.
package com.example.counselling.core.domain

// OOP: Encapsulation - each constant carries its stable database code and its
// display label, so UI code never compares raw strings.
enum class ConcernCategory(
    val code: String,
    val label: String,
) {
    STRESS("stress", "Stress"),
    ANXIETY("anxiety", "Anxiety"),
    ACADEMIC_PRESSURE("academic_pressure", "Academic Pressure"),
    RELATIONSHIPS("relationships", "Relationships"),
    SLEEP("sleep", "Sleep"),
    FINANCIAL("financial", "Financial"),
    LONELINESS("loneliness", "Loneliness"),
    OTHER("other", "Other");

    companion object {
        // OOP: Factory - one place converts database/UI text into the type.
        fun fromWire(value: String?): ConcernCategory? {
            val text = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull {
                it.code == text || it.label.lowercase() == text
            }
        }
    }
}

// OOP: Encapsulation - session mode is a value, not a free string.
enum class SessionMode(
    val code: String,
    val label: String,
) {
    IN_PERSON("in_person", "In person"),
    ONLINE("online", "Online");

    companion object {
        fun fromWire(value: String?): SessionMode? {
            val text = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull {
                it.code == text || it.label.lowercase() == text
            }
        }
    }
}

// OOP: Encapsulation - risk severity owns its ordering and "high" rule.
enum class RiskLevel(
    val code: String,
    val label: String,
    val rank: Int,
) {
    LOW("low", "Low", 1),
    MEDIUM("medium", "Medium", 2),
    HIGH("high", "High", 3);

    fun isHigh(): Boolean = this == HIGH

    companion object {
        fun fromWire(value: String?): RiskLevel? {
            val text = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull {
                it.code == text || it.label.lowercase() == text
            }
        }
    }
}

// OOP: Encapsulation - the self-check bracket owns its score window.
enum class StressBracket(
    val code: String,
    val label: String,
    val minScore: Int,
    val maxScore: Int,
) {
    LOW("low", "Low", 0, 13),
    MODERATE("moderate", "Moderate", 14, 26),
    HIGH("high", "High", 27, 40);

    fun contains(score: Int): Boolean = score in minScore..maxScore

    companion object {
        fun fromWire(value: String?): StressBracket? {
            val text = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull {
                it.code == text || it.label.lowercase() == text
            }
        }

        fun forScore(score: Int): StressBracket {
            if (score !in 0..40) {
                throw DomainException("Self-check score must be between 0 and 40.")
            }
            return entries.first { it.contains(score) }
        }
    }
}
