// UI components | Owner: shared (Beh, Jeremy, Michael)
// Reusable date + two-column time picker for booking, rescheduling and follow-ups.
package com.example.counselling.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.counselling.core.DateTimeUtils

data class TimeSlotChoice(
    val id: String,
    val startAt: String,
    val endAt: String,
)

@Composable
fun SessionSlotPicker(
    slots: List<TimeSlotChoice>,
    selectedSlotId: String?,
    onSelectSlot: (String) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(slots) {
        slots.groupBy { DateTimeUtils.formatDate(it.startAt) }
    }
    var selectedDate by remember(slots) {
        mutableStateOf(grouped.keys.firstOrNull())
    }
    val activeDate = selectedDate?.takeIf { grouped.containsKey(it) }
        ?: grouped.keys.firstOrNull()
    val visibleSlots = grouped[activeDate].orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Pick a day",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.keys.forEach { date ->
                FilterChip(
                    selected = date == activeDate,
                    onClick = {
                        if (selectedDate != date) {
                            selectedDate = date
                            onClearSelection()
                        }
                    },
                    label = { Text(date) },
                )
            }
        }

        Text(
            text = "Pick a time",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleSlots.chunked(2).forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowSlots.forEach { slot ->
                        SessionSlotOption(
                            slot = slot,
                            selected = selectedSlotId == slot.id,
                            onClick = { onSelectSlot(slot.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowSlots.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SessionSlotOption(
    slot: TimeSlotChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateTimeUtils.formatTime(slot.startAt),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Text(
                text = "to ${DateTimeUtils.formatTime(slot.endAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
