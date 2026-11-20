// Module 2 | Owner: Beh
// Pure-Kotlin State pattern shared by the appointment domain and the database
// appointment_status_transitions reference table.
package com.example.counselling.core.domain

// OOP: State pattern - each concrete status owns the transitions and actions
// allowed from that state. UI code no longer compares status strings.
sealed class AppointmentStatus(
    val wireValue: String,
    val label: String,
    val isTerminal: Boolean,
) {

    abstract fun allowedNextStatuses(): Set<AppointmentStatus>

    open fun canReschedule(): Boolean = false

    open fun canCancel(): Boolean = false

    fun canTransitionTo(next: AppointmentStatus): Boolean =
        next in allowedNextStatuses()

    override fun toString(): String = label

    // OOP: State - pending can be confirmed or declined.
    data object Pending : AppointmentStatus("pending", "Pending", isTerminal = false) {
        override fun allowedNextStatuses(): Set<AppointmentStatus> =
            setOf(Confirmed, Cancelled)

        override fun canReschedule(): Boolean = true
        override fun canCancel(): Boolean = true
    }

    // OOP: State - confirmed can be completed, marked no-show, cancelled or
    // moved by the counsellor's emergency reschedule path.
    data object Confirmed : AppointmentStatus("confirmed", "Confirmed", isTerminal = false) {
        override fun allowedNextStatuses(): Set<AppointmentStatus> =
            setOf(Completed, NoShow, Cancelled)

        override fun canReschedule(): Boolean = true
        override fun canCancel(): Boolean = true
    }

    data object Completed : AppointmentStatus("completed", "Completed", isTerminal = true) {
        override fun allowedNextStatuses(): Set<AppointmentStatus> = emptySet()
    }

    data object NoShow : AppointmentStatus("no_show", "No show", isTerminal = true) {
        override fun allowedNextStatuses(): Set<AppointmentStatus> = emptySet()
    }

    data object Cancelled : AppointmentStatus("cancelled", "Cancelled", isTerminal = true) {
        override fun allowedNextStatuses(): Set<AppointmentStatus> = emptySet()
    }

    companion object {
        // OOP: Factory - converts the database status code into the State object.
        fun fromWire(value: String?): AppointmentStatus {
            val text = value?.trim()?.lowercase()
            return entries.firstOrNull { it.wireValue == text }
                ?: throw DomainException("Unknown appointment status: ${value ?: "null"}")
        }

        fun fromWireOrNull(value: String?): AppointmentStatus? {
            val text = value?.trim()?.lowercase()
            return entries.firstOrNull { it.wireValue == text }
        }

        private val entries: List<AppointmentStatus>
            get() = listOf(Pending, Confirmed, Completed, NoShow, Cancelled)
    }
}
