// Module 1 | Owner: Beh
// Client-side validation for the shared account/registration fields.
// The same rules are enforced by supabase/schema.sql and Supabase Auth.
package com.example.counselling.core

object Validators {

    // Mirrors the PostgreSQL regex: 012-3456789 or +60123456789.
    private val phoneRegex = Regex("""^(01[0-9]-[0-9]{7}|\+601[0-9][0-9]{7,8})$""")
    private val emailRegex = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun fullName(value: String): String? {
        val name = value.trim()
        return when {
            name.isEmpty() -> "Full name is required."
            name.length < 2 -> "Full name is too short."
            name.length > 100 -> "Full name must be 100 characters or fewer."
            else -> null
        }
    }

    fun studentId(value: String): String? {
        val id = value.trim()
        return when {
            id.isEmpty() -> "Student ID is required."
            id.length < 3 -> "Student ID is too short."
            id.length > 30 -> "Student ID must be 30 characters or fewer."
            else -> null
        }
    }

    fun programme(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Programme is required."
            text.length < 2 -> "Programme is too short."
            text.length > 120 -> "Programme must be 120 characters or fewer."
            else -> null
        }
    }

    fun phone(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Phone number is required."
            !phoneRegex.matches(text) ->
                "Use a Malaysian number such as 012-3456789 or +60123456789."
            else -> null
        }
    }

    fun email(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Email is required."
            !emailRegex.matches(text) -> "Enter a valid email address."
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required."
        value.length < 8 -> "Password must be at least 8 characters."
        !value.any { it.isUpperCase() } -> "Password needs at least one uppercase letter."
        !value.any { it.isLowerCase() } -> "Password needs at least one lowercase letter."
        !value.any { it.isDigit() } -> "Password needs at least one number."
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Please confirm your password."
        password != confirm -> "Passwords do not match."
        else -> null
    }

    fun preferredMode(value: String): String? =
        if (value in AppConstants.sessionModes) null else "Choose a preferred session mode."

    fun concernCategory(value: String): String? =
        if (value in AppConstants.concernCategories) null else "Choose a concern category."

    fun required(value: String, fieldName: String): String? =
        if (value.isBlank()) "$fieldName is required." else null
}
