package com.xabier.carcareo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors

/** Mono label sitting above a field. Turns red on error. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Text(
        text = text.upcase(),
        style = LedgerText.mono10Label,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Sans helper / error text under a field or a field group. */
@Composable
fun FieldHelper(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Text(
        text = text,
        style = LedgerText.supporting,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun fieldBorderColor(focused: Boolean, isError: Boolean, primary: Boolean): Color = when {
    isError -> MaterialTheme.colorScheme.error
    focused || primary -> MaterialTheme.extraColors.ink
    else -> MaterialTheme.colorScheme.outline
}

/**
 * A bordered input box with the label above it (not a floating label). Used by
 * the task sheet and the vehicle form.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    helperText: String? = null,
    singleLine: Boolean = true,
    primary: Boolean = false,
    minHeight: Dp = 48.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LedgerText.rowTitle,
) {
    Column(modifier) {
        FieldLabel(label, isError = isError)
        Spacer(Modifier.width(6.dp))
        FieldBox(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            primary = primary,
            singleLine = singleLine,
            minHeight = minHeight,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        val support = errorText?.takeIf { isError } ?: helperText
        if (support != null) {
            Spacer(Modifier.width(4.dp))
            FieldHelper(support, Modifier.padding(top = 4.dp), isError = isError)
        }
    }
}

/** Bare bordered text box (no label). */
@Composable
fun FieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    primary: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 48.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LedgerText.rowTitle,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val border = fieldBorderColor(focused, isError, primary)
    Row(
        modifier
            .clip(LedgerCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, border, LedgerCardShape)
            .heightIn(min = minHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
            cursorBrush = SolidColor(MaterialTheme.extraColors.ink),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .onFocusChanged { focused = it.isFocused },
        )
        if (trailing != null) trailing()
    }
}

/**
 * A value box with a fixed unit suffix ("KM", "MONTHS"). Pair two of these under
 * one [FieldLabel] for the interval / warn fields.
 */
@Composable
fun NumberUnitBox(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    FieldBox(
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LedgerText.rowMeta.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        modifier = modifier,
        trailing = {
            Text(
                text = unit.upcase(),
                style = LedgerText.rowMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
