// Core infrastructure | Owner: shared (Beh, Jeremy, Michael)
// Abstract parent for every Supabase repository. Subclasses implement only
// their query/mutation bodies; this class owns the common try/catch,
// ErrorTranslator call and Result wrapping.
package com.example.counselling.core.data

import com.example.counselling.core.domain.DomainException
import io.github.jan.supabase.SupabaseClient

// OOP: Template Method - execute() defines the fixed error-handling skeleton;
// each repository supplies only the database operation in runQuery's lambda.
// OOP: Abstraction - ViewModels will depend on repository interfaces, never on
// this Supabase-specific base class.
abstract class BaseSupabaseRepository(
    protected val supabase: SupabaseClient,
    protected val errorTranslator: ErrorTranslator = ErrorTranslator(),
) {

    protected suspend fun <T> runQuery(
        fallback: String,
        query: suspend SupabaseClient.() -> T,
    ): Result<T> = try {
        Result.success(supabase.query())
    } catch (error: Exception) {
        Result.failure(
            Exception(
                messageFor(error, fallback),
                error,
            ),
        )
    }

    protected fun <T> failure(
        error: Throwable,
        fallback: String,
    ): Result<T> = Result.failure(
        Exception(messageFor(error, fallback), error),
    )

    protected fun messageFor(
        error: Throwable,
        fallback: String,
    ): String =
        if (error is DomainException) {
            error.message ?: fallback
        } else {
            errorTranslator.translate(error, fallback)
        }
}
