// Core | Owner: shared (Beh, Jeremy, Michael)
// Keeps the current profile and role in memory, validates that the account is
// active, and gives the rest of the app one place to sign in/out.
package com.example.counselling.core

import com.example.counselling.feature.auth.data.AuthRepository
import com.example.counselling.feature.student.data.ProfileRepository
import com.example.counselling.feature.student.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject

sealed interface SessionState {
    data object Loading : SessionState

    /** notice is used for a deactivated account or a one-off login message. */
    data class SignedOut(val notice: UiMessage? = null) : SessionState
    data class SignedIn(val profile: Profile) : SessionState
}

class SessionManager(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var initialized = false

    val currentProfile: Profile?
        get() = (_state.value as? SessionState.SignedIn)?.profile

    suspend fun initialize() {
        if (initialized) return
        initialized = true
        _state.value = SessionState.Loading
        runCatching { authRepository.awaitReady() }
        restoreProfileOrSignOut()
    }

    /**
     * Returns success(false) when Supabase requires email confirmation and no
     * session was created. Returns success(true) when the user is signed in.
     */
    suspend fun signUp(
        email: String,
        password: String,
        metadata: JsonObject,
    ): Result<Boolean> {
        val authResult = authRepository.signUp(email, password, metadata)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Registration failed."))
        }

        if (authRepository.currentUserId() == null) {
            _state.value = SessionState.SignedOut()
            return Result.success(false)
        }

        return loadSignedInProfile().fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        val authResult = authRepository.signIn(email, password)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign in failed."))
        }
        return loadSignedInProfile()
    }

    suspend fun signOut() {
        authRepository.signOut()
        _state.value = SessionState.SignedOut()
    }

    /** Re-reads the profile after a profile edit. */
    suspend fun refreshProfile(): Result<Unit> = loadSignedInProfile()

    fun clearNotice() {
        val current = _state.value
        if (current is SessionState.SignedOut && current.notice != null) {
            _state.value = SessionState.SignedOut()
        }
    }

    private suspend fun restoreProfileOrSignOut() {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = SessionState.SignedOut()
            return
        }
        loadProfile(userId)
    }

    private suspend fun loadSignedInProfile(): Result<Unit> {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _state.value = SessionState.SignedOut()
            return Result.failure(Exception("Your session could not be created. Please sign in again."))
        }
        return loadProfile(userId)
    }

    private suspend fun loadProfile(userId: String): Result<Unit> {
        val result = profileRepository.getProfile(userId)
        val profile = result.getOrElse { error ->
            authRepository.signOut()
            _state.value = SessionState.SignedOut()
            return Result.failure(Exception(ErrorMapper.message(error, "Could not load your profile.")))
        }

        if (profile == null) {
            authRepository.signOut()
            val message = "Your profile could not be found. Please contact the administrator."
            _state.value = SessionState.SignedOut(UiMessage(message, MessageKind.ERROR))
            return Result.failure(Exception(message))
        }

        if (!profile.isActive) {
            authRepository.signOut()
            val message = "Your account has been deactivated. Please contact the counselling office."
            _state.value = SessionState.SignedOut(UiMessage(message, MessageKind.ERROR))
            return Result.failure(Exception(message))
        }

        if (profile.userRole == null) {
            authRepository.signOut()
            val message = "Your account role is not recognised. Please contact the administrator."
            _state.value = SessionState.SignedOut(UiMessage(message, MessageKind.ERROR))
            return Result.failure(Exception(message))
        }

        _state.value = SessionState.SignedIn(profile)
        return Result.success(Unit)
    }
}
