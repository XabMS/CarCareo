package com.xabier.carcareo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors

private val paper: Color get() = Color(0xFFF6F1E7)

/** Ink-on-paper primary action (handoff: primary buttons are NOT accent-coloured). */
@Composable
fun LedgerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
) {
    val ink = MaterialTheme.extraColors.ink
    Row(
        modifier
            .height(height)
            .clip(LedgerCardShape)
            .background(if (enabled) ink else ink.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = paper, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text.upcase(), style = LedgerText.buttonLabel, color = paper)
    }
}

/** Outlined ink action — "UPDATE", "CANCEL". */
@Composable
fun LedgerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 40.dp,
) {
    val ink = MaterialTheme.extraColors.ink
    Row(
        modifier
            .height(height)
            .clip(LedgerCardShape)
            .border(1.dp, if (enabled) ink else ink.copy(alpha = 0.4f), LedgerCardShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.upcase(), style = LedgerText.buttonLabelSm, color = ink)
    }
}

/** Text-only action in the accent colour — "SELECT ALL OVERDUE", "LOG". */
@Composable
fun LedgerTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text.upcase(),
        style = LedgerText.buttonLabelSm,
        color = color,
        modifier = modifier
            .clip(LedgerCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** The floating action bar (garage FAB, add-task FAB). 52dp, ink, drop shadow. */
@Composable
fun FloatingActionBar(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.extraColors.ink
    Row(
        modifier
            .shadow(10.dp, LedgerCardShape, spotColor = ink, ambientColor = ink)
            .clip(LedgerCardShape)
            .background(ink)
            .clickable(onClick = onClick)
            .height(52.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = paper, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text.upcase(), style = LedgerText.buttonLabel, color = paper)
    }
}
