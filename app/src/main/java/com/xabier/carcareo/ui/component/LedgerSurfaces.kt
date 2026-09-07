package com.xabier.carcareo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.taskStatusColors

/** The card shape shared by every framed block, table and bar. */
val LedgerCardShape: Shape = RoundedCornerShape(4.dp)

/**
 * Every framed block in the "Workshop ledger" design: paper-white ground, 1dp
 * hairline border, 4dp radius, no elevation — the border does the work.
 */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    shape: Shape = LedgerCardShape,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}

/** Mono uppercase label + a hairline rule filling the rest of the row. */
@Composable
fun SectionLabelRow(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.upcase(),
            style = LedgerText.sectionLabel,
            color = labelColor,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = trailing,
                style = LedgerText.rowMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 1dp hairline between rows inside a table. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * The 6dp status stripe that replaces the semaphore dot: overdue share, then
 * upcoming share, then a neutral remainder. Fractions are of the whole plan.
 */
@Composable
fun StatusStripe(
    overdueFraction: Float,
    upcomingFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = taskStatusColors(isSystemInDarkTheme())
    val o = overdueFraction.coerceIn(0f, 1f)
    val u = upcomingFraction.coerceIn(0f, 1f - o)
    val rem = (1f - o - u).coerceAtLeast(0f)
    Row(
        modifier
            .fillMaxWidth()
            .height(6.dp),
    ) {
        if (o > 0f) Box(Modifier.fillMaxHeight().weight(o).background(colors.overdue))
        if (u > 0f) Box(Modifier.fillMaxHeight().weight(u).background(colors.upcoming))
        if (rem > 0f) Box(Modifier.fillMaxHeight().weight(rem).background(MaterialTheme.colorScheme.outline))
    }
}

/** Small interval-consumption bar: neutral track, status-coloured fill. */
@Composable
fun MiniBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
) {
    Box(
        modifier
            .width(width)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color),
        )
    }
}
