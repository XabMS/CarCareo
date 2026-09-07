package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LabeledField
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.LedgerPrimaryButton
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.format.formatDate
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.VehicleFormViewModel
import com.xabier.carcareo.ui.vehicle.icon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: VehicleFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }

    val nameMissing = stringResource(R.string.error_name_required)
    val kmInvalid = stringResource(R.string.error_km_invalid)
    val fixErrors = stringResource(R.string.form_fix_errors)
    LaunchedEffect(state.submitCount) {
        if (state.submitCount == 0) return@LaunchedEffect
        if (state.nameError || state.kmError) {
            scrollState.animateScrollTo(0)
            snackbar.showSnackbar(
                when {
                    state.nameError && state.kmError -> fixErrors
                    state.nameError -> nameMissing
                    else -> kmInvalid
                },
            )
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            LedgerAppBar(
                title = stringResource(
                    if (state.isEdit) R.string.screen_edit_vehicle else R.string.screen_new_vehicle,
                ),
                context = if (state.isEdit) null
                else stringResource(R.string.form_step, 1, 2),
                navigationIcon = {
                    LedgerIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                },
                progressFilled = if (state.isEdit) null else 1,
                progressTotal = 2,
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CategoryTiles(
                    selected = state.category,
                    onSelected = { c -> viewModel.edit { it.copy(category = c) } },
                )

                LabeledField(
                    label = stringResource(R.string.field_name),
                    value = state.name,
                    onValueChange = { v -> viewModel.edit { it.copy(name = v, nameError = false) } },
                    isError = state.nameError,
                    errorText = stringResource(R.string.error_name_required),
                    primary = true,
                )

                Column {
                    SectionLabelRow(stringResource(R.string.form_section_vehicle))
                    Spacer(Modifier.height(4.dp))
                    LedgerCard {
                        FormTextRow(
                            label = stringResource(R.string.field_make),
                            value = state.make,
                            onValueChange = { v -> viewModel.edit { it.copy(make = v) } },
                        )
                        HairlineDivider()
                        FormTextRow(
                            label = stringResource(R.string.field_model),
                            value = state.model,
                            onValueChange = { v -> viewModel.edit { it.copy(model = v) } },
                        )
                        HairlineDivider()
                        FormTextRow(
                            label = stringResource(R.string.field_year),
                            value = state.year,
                            onValueChange = { v -> viewModel.edit { it.copy(year = v.filter(Char::isDigit).take(4)) } },
                            mono = true,
                            keyboardType = KeyboardType.Number,
                        )
                        HairlineDivider()
                        FormTextRow(
                            label = stringResource(R.string.field_plate),
                            value = state.plate,
                            onValueChange = { v -> viewModel.edit { it.copy(plate = v) } },
                            mono = true,
                        )
                        HairlineDivider()
                        FormDateRow(
                            label = stringResource(R.string.form_row_bought),
                            value = state.purchaseDate,
                            onValueChange = { d -> viewModel.edit { it.copy(purchaseDate = d) } },
                        )
                    }
                }

                Column {
                    SectionLabelRow(stringResource(R.string.form_section_mileage))
                    Spacer(Modifier.height(4.dp))
                    LedgerCard {
                        FormTextRow(
                            label = stringResource(R.string.form_row_current),
                            value = state.lastConfirmedKm,
                            onValueChange = { v ->
                                viewModel.edit { it.copy(lastConfirmedKm = v.filter(Char::isDigit), kmError = false) }
                            },
                            mono = true,
                            keyboardType = KeyboardType.Number,
                            suffix = stringResource(R.string.unit_km),
                            isError = state.kmError,
                        )
                        HairlineDivider()
                        FormDateRow(
                            label = stringResource(R.string.form_row_read_on),
                            value = state.lastConfirmedKmDate,
                            onValueChange = { d -> viewModel.edit { it.copy(lastConfirmedKmDate = d) } },
                        )
                        HairlineDivider()
                        FormTextRow(
                            label = stringResource(R.string.form_row_per_year),
                            value = state.annualKmEstimate,
                            onValueChange = { v -> viewModel.edit { it.copy(annualKmEstimate = v.filter(Char::isDigit)) } },
                            mono = true,
                            keyboardType = KeyboardType.Number,
                            suffix = stringResource(R.string.unit_km),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.form_mileage_help),
                        style = LedgerText.supporting,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LabeledField(
                    label = stringResource(R.string.field_notes),
                    value = state.notes,
                    onValueChange = { v -> viewModel.edit { it.copy(notes = v) } },
                    singleLine = false,
                    minHeight = 72.dp,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                HairlineDivider(Modifier.background(MaterialTheme.colorScheme.outline))
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    LedgerPrimaryButton(
                        text = stringResource(
                            if (state.isEdit) R.string.action_save else R.string.form_save_setup,
                        ),
                        onClick = { viewModel.save(onSaved) },
                        icon = if (state.isEdit) null else Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CategoryTiles(
    selected: VehicleCategory,
    onSelected: (VehicleCategory) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VehicleCategory.entries.forEach { category ->
            val isSel = category == selected
            val border = if (isSel) MaterialTheme.extraColors.ink else MaterialTheme.colorScheme.outline
            val content = if (isSel) MaterialTheme.extraColors.ink else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, border, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .clickable { onSelected(category) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(category.icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(category.shortLabelRes).upcase(),
                    style = LedgerText.tag,
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

private val VehicleCategory.shortLabelRes: Int
    get() = when (this) {
        VehicleCategory.MOTO_TERMICA -> R.string.category_short_moto
        VehicleCategory.COCHE_TERMICO -> R.string.category_short_coche_termico
        VehicleCategory.COCHE_ELECTRICO -> R.string.category_short_coche_electrico
    }

@Composable
private fun FormTextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    isError: Boolean = false,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.upcase(),
            Modifier.width(78.dp),
            style = LedgerText.rowMeta,
            color = if (isError) MaterialTheme.colorScheme.error else onVar,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = (if (mono) LedgerText.rowMeta.copy(fontSize = LedgerText.rowTitle.fontSize) else LedgerText.rowTitle)
                .merge(TextStyle(color = onSurface)),
            cursorBrush = SolidColor(MaterialTheme.extraColors.ink),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.field_optional),
                        style = LedgerText.rowTitle,
                        color = onVar,
                    )
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        if (suffix != null) {
            Text(suffix.upcase(), style = LedgerText.rowMeta, color = onVar)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDateRow(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showDialog = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.upcase(), Modifier.width(78.dp), style = LedgerText.rowMeta, color = onVar)
        Text(
            value?.let { formatDate(it) } ?: stringResource(R.string.field_optional),
            Modifier.weight(1f),
            style = LedgerText.rowTitle,
            color = if (value != null) MaterialTheme.colorScheme.onSurface else onVar,
        )
        Icon(Icons.Filled.Event, contentDescription = null, tint = onVar, modifier = Modifier.size(18.dp))
    }

    if (showDialog) {
        val initialMillis = (value ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}
