// Home | Owner: shared (Beh, Jeremy, Michael)
// Provides the signed-in user's appointment snapshot for the role dashboard.
// Additional admin/counsellor aggregate stats are added with their modules.
package com.example.counselling.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.counselling.feature.appointment.data.AppointmentRepository
import com.example.counselling.feature.appointment.model.Appointment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
)

class HomeViewModel(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.appointments.isEmpty(), error = null) }
            repository.getAppointments()
                .onSuccess { appointments ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        error = null,
                        appointments = appointments,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Could not load appointments.",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: AppointmentRepository) = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
