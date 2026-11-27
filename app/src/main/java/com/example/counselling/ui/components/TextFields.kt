// UI components | Owner: shared (Beh, Jeremy, Michael)
// Form inputs with inline errors, blur validation, password visibility,
// dropdowns and Material 3 date/time pickers.
package com.example.counselling.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.counselling.core.DateTimeUtils

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: androidx.compose.ui.text.input.ImeAction =
        androidx.compose.ui.text.input.ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onFocusLost: (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused) onFocusLost?.invoke()
                wasFocused = focusState.isFocused
            },
        enabled = enabled,
        label = { Text(label) },
        singleLine = singleLine,
        maxLines = maxLines,
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            supportingText != null -> {
                { Text(supportingText) }
            }
            else -> null
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
    placeholder: String = "Select",
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = error != null,
            supportingText = when {
                error != null -> {
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                supportingText != null -> {
                    { Text(supportingText) }
                }
                else -> null
            },
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open $label options")
            },
            shape = MaterialTheme.shapes.medium,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true },
        )
    }

    if (expanded) {
        AppBottomSheet(
            onDismissRequest = { expanded = false },
            title = label,
        ) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == selected,
                            role = Role.RadioButton,
                            onClick = {
                                expanded = false
                                onSelect(option)
                            },
                        )
                        .heightIn(min = 52.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == selected,
                        onClick = null,
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Single-choice control for short option sets. Segmented buttons keep every
 * choice visible without opening a menu, which suits two to four options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedChoiceField(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    enabled = enabled,
                ) {
                    Text(
                        text = option,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        val message = error ?: supportingText
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedMillis: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val display = if (selectedMillis == null) "" else DateTimeUtils.formatDate(selectedMillis)

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Choose a date") },
            isError = error != null,
            supportingText = when {
                error != null -> {
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                supportingText != null -> {
                    { Text(supportingText) }
                }
                else -> null
            },
            trailingIcon = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Open date picker")
            },
            shape = MaterialTheme.shapes.medium,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showDialog = true },
        )
    }

    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let(onDateSelected)
                        showDialog = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
