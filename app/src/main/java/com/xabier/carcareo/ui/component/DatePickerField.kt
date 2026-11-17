package com.xabier.carcareo.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import com.xabier.carcareo.ui.icon.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xabier.carcareo.R
import com.xabier.carcareo.ui.format.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A read-only text field that opens a Material 3 date picker when tapped.
 * [value] null renders an empty field (used for optional dates).
 *
 * Implementation note: a read-only [OutlinedTextField] does not emit click
 * events, so a transparent [Box] on top captures the tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    allowFuture: Boolean = false,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.let { formatDate(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = supportingText?.let { { Text(it) } },
            isError = isError,
            trailingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = label,
                ) { showDialog = true }
                .semantics { contentDescription = label },
        )
    }

    if (showDialog) {
        LedgerDatePickerDialog(
            initial = value,
            onPicked = onValueChange,
            onDismiss = { showDialog = false },
            allowFuture = allowFuture,
        )
    }
}

/**
 * The app's one date picker dialog. Every date the app asks for is a fact about
 * the past (a reading, a purchase, work done), so future days are not selectable
 * unless [allowFuture] says otherwise — better than accepting the tap and
 * complaining afterwards.
 *
 * The Material picker works in UTC-midnight millis; the conversions here keep it
 * on calendar dates so no time zone can shift the picked day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDatePickerDialog(
    initial: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    allowFuture: Boolean = false,
) {
    val today = LocalDate.now()
    val selectableDates = remember(allowFuture, today) {
        if (allowFuture) {
            DatePickerDefaults.AllDates
        } else {
            val lastMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= lastMillis

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
