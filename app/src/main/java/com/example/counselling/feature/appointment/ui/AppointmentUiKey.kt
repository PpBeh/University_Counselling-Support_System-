// Module 2 | Owner: Beh
package com.example.counselling.feature.appointment.ui

import com.example.counselling.feature.appointment.domain.Appointment

// Compose/ LazyColumn uses equality for keys and parameter skipping. Appointment
// equality is identity-by-id, so include the visible workflow fields in the UI
// key to force a card to recompose after confirm/reschedule/cancel.
fun Appointment.uiKey(): String =
    "$id:${status.wireValue}:$lastEvent:$slotId:${updatedAt?.toString().orEmpty()}"
