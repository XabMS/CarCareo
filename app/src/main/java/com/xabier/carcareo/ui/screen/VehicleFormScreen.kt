package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.DatePickerField
import com.xabier.carcareo.ui.vehicle.VehicleFormViewModel
import com.xabier.carcareo.ui.vehicle.labelRes

/**
 * Add / edit vehicle form. Same screen for both: the ViewModel decides based on
 * whether a vehicleId was passed in the route.
 */
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

    // A rejected save can leave the error on a field scrolled out of view (it
    // happened to the user), so on a failed submit we scroll back to the top and
    // show a message saying what to fix.
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEdit) R.string.screen_edit_vehicle
                            else R.string.screen_new_vehicle,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = { viewModel.save(onSaved) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.edit { it.copy(name = v, nameError = false) } },
                label = { Text(stringResource(R.string.field_name)) },
                placeholder = { Text(stringResource(R.string.field_name_hint)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryDropdown(
                selected = state.category,
                onSelected = { c -> viewModel.edit { it.copy(category = c) } },
            )

            OutlinedTextField(
                value = state.make,
                onValueChange = { v -> viewModel.edit { it.copy(make = v) } },
                label = { Text(stringResource(R.string.field_make)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = { v -> viewModel.edit { it.copy(model = v) } },
                label = { Text(stringResource(R.string.field_model)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.year,
                onValueChange = { v -> viewModel.edit { it.copy(year = v.filter(Char::isDigit).take(4)) } },
                label = { Text(stringResource(R.string.field_year)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.plate,
                onValueChange = { v -> viewModel.edit { it.copy(plate = v) } },
                label = { Text(stringResource(R.string.field_plate)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.lastConfirmedKm,
                onValueChange = { v ->
                    viewModel.edit { it.copy(lastConfirmedKm = v.filter(Char::isDigit), kmError = false) }
                },
                label = { Text(stringResource(R.string.field_last_km)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.kmError,
                supportingText = if (state.kmError) {
                    { Text(stringResource(R.string.error_km_invalid)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DatePickerField(
                label = stringResource(R.string.field_last_km_date),
                value = state.lastConfirmedKmDate,
                onValueChange = { d -> viewModel.edit { it.copy(lastConfirmedKmDate = d) } },
            )

            OutlinedTextField(
                value = state.annualKmEstimate,
                onValueChange = { v ->
                    viewModel.edit { it.copy(annualKmEstimate = v.filter(Char::isDigit)) }
                },
                label = { Text(stringResource(R.string.field_annual_km)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text(stringResource(R.string.field_annual_km_help)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DatePickerField(
                label = stringResource(R.string.field_purchase_date),
                value = state.purchaseDate,
                supportingText = stringResource(R.string.field_optional),
                onValueChange = { d -> viewModel.edit { it.copy(purchaseDate = d) } },
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { v -> viewModel.edit { it.copy(notes = v) } },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: VehicleCategory,
    onSelected: (VehicleCategory) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            VehicleCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}
