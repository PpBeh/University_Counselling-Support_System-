// Module 5 | Owner: Michael
package com.example.counselling.feature.resource.domain

import com.example.counselling.core.domain.DomainException

// OOP: Enum - each media type owns its wire code and display label.
enum class ResourceType(val code: String, val label: String) {
    ARTICLE("article", "Article"),
    VIDEO("video", "Video"),
    AUDIO("audio", "Audio"),
    PDF("pdf", "PDF"),
    HOTLINE("hotline", "Hotline"),
    ;

    companion object {
        fun fromWire(raw: String): ResourceType =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true) }
                ?: throw DomainException("Unknown resource type: $raw")

        fun fromLabel(label: String): ResourceType =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: throw DomainException("Choose a valid media type.")
    }
}
