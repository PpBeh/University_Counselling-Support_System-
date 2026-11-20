// Core | Owner: shared (Beh, Jeremy, Michael)
// OOP: Delegation - this thin facade keeps the SessionManager call site stable
// while all feature repositories inject and reuse the ErrorTranslator class.
package com.example.counselling.core

import com.example.counselling.core.data.ErrorTranslator

object ErrorMapper {

    private val translator = ErrorTranslator()

    fun message(
        error: Throwable,
        fallback: String = "Something went wrong. Please try again.",
    ): String = translator.translate(error, fallback)
}
