// Vendored icon accessors follow androidx material-icons' generated pattern:
// an `Icons.Filled.*` extension property whose getter ignores the receiver (it
// only scopes the call syntax) backed by a top-level `_name` field. Both are
// deliberate here, so silence the IDE inspections that flag the upstream shape.
@file:Suppress("UnusedReceiverParameter", "ObjectPropertyName")

package com.xabier.carcareo.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of `Icons.Filled.*` glyphs the app actually uses that are NOT in
 * `material-icons-core` (which ships with material3). Vendored verbatim from
 * `androidx.compose.material:material-icons-extended` 1.7.8 (Apache-2.0) so we can
 * drop that ~5 000-icon dependency — it was the bulk of the release dex.
 *
 * The path data below is copied unchanged from the upstream sources; only the
 * `materialIcon` / `materialPath` helpers are local re-implementations of the
 * (identical) upstream ones. To add another icon, copy its `materialPath { … }`
 * block from the extended-icons source jar.
 */

private inline fun materialIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

private inline fun ImageVector.Builder.materialPath(
    pathFillType: PathFillType = PathFillType.NonZero,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector.Builder = path(
    fill = SolidColor(Color.Black),
    stroke = null,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = pathFillType,
    pathBuilder = pathBuilder,
)

private var _attachFile: ImageVector? = null
val Icons.Filled.AttachFile: ImageVector
    get() = _attachFile ?: materialIcon("Filled.AttachFile") {
        materialPath {
            moveTo(16.5f, 6.0f)
            verticalLineToRelative(11.5f)
            curveToRelative(0.0f, 2.21f, -1.79f, 4.0f, -4.0f, 4.0f)
            reflectiveCurveToRelative(-4.0f, -1.79f, -4.0f, -4.0f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
            reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
            verticalLineToRelative(10.5f)
            curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            verticalLineTo(6.0f)
            horizontalLineTo(10.0f)
            verticalLineToRelative(9.5f)
            curveToRelative(0.0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
            reflectiveCurveToRelative(2.5f, -1.12f, 2.5f, -2.5f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
            reflectiveCurveTo(7.0f, 2.79f, 7.0f, 5.0f)
            verticalLineToRelative(12.5f)
            curveToRelative(0.0f, 3.04f, 2.46f, 5.5f, 5.5f, 5.5f)
            reflectiveCurveToRelative(5.5f, -2.46f, 5.5f, -5.5f)
            verticalLineTo(6.0f)
            horizontalLineToRelative(-1.5f)
            close()
        }
    }.also { _attachFile = it }

private var _checklist: ImageVector? = null
val Icons.Filled.Checklist: ImageVector
    get() = _checklist ?: materialIcon("Filled.Checklist") {
        materialPath {
            moveTo(22.0f, 7.0f)
            horizontalLineToRelative(-9.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(9.0f)
            verticalLineTo(7.0f)
            close()
            moveTo(22.0f, 15.0f)
            horizontalLineToRelative(-9.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(9.0f)
            verticalLineTo(15.0f)
            close()
            moveTo(5.54f, 11.0f)
            lineTo(2.0f, 7.46f)
            lineToRelative(1.41f, -1.41f)
            lineToRelative(2.12f, 2.12f)
            lineToRelative(4.24f, -4.24f)
            lineToRelative(1.41f, 1.41f)
            lineTo(5.54f, 11.0f)
            close()
            moveTo(5.54f, 19.0f)
            lineTo(2.0f, 15.46f)
            lineToRelative(1.41f, -1.41f)
            lineToRelative(2.12f, 2.12f)
            lineToRelative(4.24f, -4.24f)
            lineToRelative(1.41f, 1.41f)
            lineTo(5.54f, 19.0f)
            close()
        }
    }.also { _checklist = it }

private var _contentCopy: ImageVector? = null
val Icons.Filled.ContentCopy: ImageVector
    get() = _contentCopy ?: materialIcon("Filled.ContentCopy") {
        materialPath {
            moveTo(16.0f, 1.0f)
            lineTo(4.0f, 1.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            lineTo(4.0f, 3.0f)
            horizontalLineToRelative(12.0f)
            lineTo(16.0f, 1.0f)
            close()
            moveTo(19.0f, 5.0f)
            lineTo(8.0f, 5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 21.0f)
            lineTo(8.0f, 21.0f)
            lineTo(8.0f, 7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineToRelative(14.0f)
            close()
        }
    }.also { _contentCopy = it }

private var _dashboard: ImageVector? = null
val Icons.Filled.Dashboard: ImageVector
    get() = _dashboard ?: materialIcon("Filled.Dashboard") {
        materialPath {
            moveTo(3.0f, 13.0f)
            horizontalLineToRelative(8.0f)
            lineTo(11.0f, 3.0f)
            lineTo(3.0f, 3.0f)
            verticalLineToRelative(10.0f)
            close()
            moveTo(3.0f, 21.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-6.0f)
            lineTo(3.0f, 15.0f)
            verticalLineToRelative(6.0f)
            close()
            moveTo(13.0f, 21.0f)
            horizontalLineToRelative(8.0f)
            lineTo(21.0f, 11.0f)
            horizontalLineToRelative(-8.0f)
            verticalLineToRelative(10.0f)
            close()
            moveTo(13.0f, 3.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(8.0f)
            lineTo(21.0f, 3.0f)
            horizontalLineToRelative(-8.0f)
            close()
        }
    }.also { _dashboard = it }

private var _directionsCar: ImageVector? = null
val Icons.Filled.DirectionsCar: ImageVector
    get() = _directionsCar ?: materialIcon("Filled.DirectionsCar") {
        materialPath {
            moveTo(18.92f, 6.01f)
            curveTo(18.72f, 5.42f, 18.16f, 5.0f, 17.5f, 5.0f)
            horizontalLineToRelative(-11.0f)
            curveToRelative(-0.66f, 0.0f, -1.21f, 0.42f, -1.42f, 1.01f)
            lineTo(3.0f, 12.0f)
            verticalLineToRelative(8.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(1.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-1.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(1.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-8.0f)
            lineToRelative(-2.08f, -5.99f)
            close()
            moveTo(6.5f, 16.0f)
            curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(5.67f, 13.0f, 6.5f, 13.0f)
            reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveTo(7.33f, 16.0f, 6.5f, 16.0f)
            close()
            moveTo(17.5f, 16.0f)
            curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
            reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
            close()
            moveTo(5.0f, 11.0f)
            lineToRelative(1.5f, -4.5f)
            horizontalLineToRelative(11.0f)
            lineTo(19.0f, 11.0f)
            lineTo(5.0f, 11.0f)
            close()
        }
    }.also { _directionsCar = it }

private var _download: ImageVector? = null
val Icons.Filled.Download: ImageVector
    get() = _download ?: materialIcon("Filled.Download") {
        materialPath {
            moveTo(5.0f, 20.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(20.0f)
            close()
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(6.0f)
            horizontalLineTo(5.0f)
            lineToRelative(7.0f, 7.0f)
            lineTo(19.0f, 9.0f)
            close()
        }
    }.also { _download = it }

private var _dragIndicator: ImageVector? = null
val Icons.Filled.DragIndicator: ImageVector
    get() = _dragIndicator ?: materialIcon("Filled.DragIndicator") {
        materialPath {
            moveTo(11.0f, 18.0f)
            curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
            reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
            reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            close()
            moveTo(9.0f, 10.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(9.0f, 4.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(15.0f, 8.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            close()
            moveTo(15.0f, 10.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(15.0f, 16.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
        }
    }.also { _dragIndicator = it }

private var _editNote: ImageVector? = null
val Icons.Filled.EditNote: ImageVector
    get() = _editNote ?: materialIcon("Filled.EditNote") {
        materialPath {
            moveTo(3.0f, 10.0f)
            horizontalLineToRelative(11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(3.0f)
            verticalLineTo(10.0f)
            close()
            moveTo(3.0f, 8.0f)
            horizontalLineToRelative(11.0f)
            verticalLineTo(6.0f)
            horizontalLineTo(3.0f)
            verticalLineTo(8.0f)
            close()
            moveTo(3.0f, 16.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(3.0f)
            verticalLineTo(16.0f)
            close()
            moveTo(18.01f, 12.87f)
            lineToRelative(0.71f, -0.71f)
            curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
            lineToRelative(0.71f, 0.71f)
            curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0.0f, 1.41f)
            lineToRelative(-0.71f, 0.71f)
            lineTo(18.01f, 12.87f)
            close()
            moveTo(17.3f, 13.58f)
            lineToRelative(-5.3f, 5.3f)
            verticalLineTo(21.0f)
            horizontalLineToRelative(2.12f)
            lineToRelative(5.3f, -5.3f)
            lineTo(17.3f, 13.58f)
            close()
        }
    }.also { _editNote = it }

private var _electricCar: ImageVector? = null
val Icons.Filled.ElectricCar: ImageVector
    get() = _electricCar ?: materialIcon("Filled.ElectricCar") {
        materialPath {
            moveTo(18.92f, 2.01f)
            curveTo(18.72f, 1.42f, 18.16f, 1.0f, 17.5f, 1.0f)
            horizontalLineToRelative(-11.0f)
            curveTo(5.84f, 1.0f, 5.29f, 1.42f, 5.08f, 2.01f)
            lineTo(3.0f, 8.0f)
            verticalLineToRelative(8.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(1.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-1.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(1.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineTo(8.0f)
            lineTo(18.92f, 2.01f)
            close()
            moveTo(6.5f, 12.0f)
            curveTo(5.67f, 12.0f, 5.0f, 11.33f, 5.0f, 10.5f)
            reflectiveCurveTo(5.67f, 9.0f, 6.5f, 9.0f)
            reflectiveCurveTo(8.0f, 9.67f, 8.0f, 10.5f)
            reflectiveCurveTo(7.33f, 12.0f, 6.5f, 12.0f)
            close()
            moveTo(17.5f, 12.0f)
            curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(16.67f, 9.0f, 17.5f, 9.0f)
            reflectiveCurveTo(19.0f, 9.67f, 19.0f, 10.5f)
            reflectiveCurveTo(18.33f, 12.0f, 17.5f, 12.0f)
            close()
            moveTo(5.0f, 7.0f)
            lineToRelative(1.5f, -4.5f)
            horizontalLineToRelative(11.0f)
            lineTo(19.0f, 7.0f)
            horizontalLineTo(5.0f)
            close()
        }
        materialPath {
            moveTo(7.0f, 20.0f)
            lineToRelative(4.0f, 0.0f)
            lineToRelative(0.0f, -2.0f)
            lineToRelative(6.0f, 3.0f)
            lineToRelative(-4.0f, 0.0f)
            lineToRelative(0.0f, 2.0f)
            close()
        }
    }.also { _electricCar = it }

private var _errorOutline: ImageVector? = null
val Icons.Filled.ErrorOutline: ImageVector
    get() = _errorOutline ?: materialIcon("Filled.ErrorOutline") {
        materialPath {
            moveTo(11.0f, 15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(11.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
            close()
        }
    }.also { _errorOutline = it }

private var _event: ImageVector? = null
val Icons.Filled.Event: ImageVector
    get() = _event ?: materialIcon("Filled.Event") {
        materialPath {
            moveTo(17.0f, 12.0f)
            horizontalLineToRelative(-5.0f)
            verticalLineToRelative(5.0f)
            horizontalLineToRelative(5.0f)
            verticalLineToRelative(-5.0f)
            close()
            moveTo(16.0f, 1.0f)
            verticalLineToRelative(2.0f)
            lineTo(8.0f, 3.0f)
            lineTo(8.0f, 1.0f)
            lineTo(6.0f, 1.0f)
            verticalLineToRelative(2.0f)
            lineTo(5.0f, 3.0f)
            curveToRelative(-1.11f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
            lineTo(3.0f, 19.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineToRelative(-1.0f)
            lineTo(18.0f, 1.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(19.0f, 19.0f)
            lineTo(5.0f, 19.0f)
            lineTo(5.0f, 8.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(11.0f)
            close()
        }
    }.also { _event = it }

private var _history: ImageVector? = null
val Icons.Filled.History: ImageVector
    get() = _history ?: materialIcon("Filled.History") {
        materialPath {
            moveTo(13.0f, 3.0f)
            curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
            lineTo(1.0f, 12.0f)
            lineToRelative(3.89f, 3.89f)
            lineToRelative(0.07f, 0.14f)
            lineTo(9.0f, 12.0f)
            lineTo(6.0f, 12.0f)
            curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
            reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
            reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
            curveToRelative(-1.93f, 0.0f, -3.68f, -0.79f, -4.94f, -2.06f)
            lineToRelative(-1.42f, 1.42f)
            curveTo(8.27f, 19.99f, 10.51f, 21.0f, 13.0f, 21.0f)
            curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
            reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f)
            close()
            moveTo(12.0f, 8.0f)
            verticalLineToRelative(5.0f)
            lineToRelative(4.28f, 2.54f)
            lineToRelative(0.72f, -1.21f)
            lineToRelative(-3.5f, -2.08f)
            lineTo(13.5f, 8.0f)
            lineTo(12.0f, 8.0f)
            close()
        }
    }.also { _history = it }

private var _inventory2: ImageVector? = null
val Icons.Filled.Inventory2: ImageVector
    get() = _inventory2 ?: materialIcon("Filled.Inventory2") {
        materialPath {
            moveTo(20.0f, 2.0f)
            horizontalLineTo(4.0f)
            curveTo(3.0f, 2.0f, 2.0f, 2.9f, 2.0f, 4.0f)
            verticalLineToRelative(3.01f)
            curveTo(2.0f, 7.73f, 2.43f, 8.35f, 3.0f, 8.7f)
            verticalLineTo(20.0f)
            curveToRelative(0.0f, 1.1f, 1.1f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(0.9f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(8.7f)
            curveToRelative(0.57f, -0.35f, 1.0f, -0.97f, 1.0f, -1.69f)
            verticalLineTo(4.0f)
            curveTo(22.0f, 2.9f, 21.0f, 2.0f, 20.0f, 2.0f)
            close()
            moveTo(15.0f, 14.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(6.0f)
            verticalLineTo(14.0f)
            close()
            moveTo(20.0f, 7.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(4.0f)
            horizontalLineToRelative(16.0f)
            verticalLineTo(7.0f)
            close()
        }
    }.also { _inventory2 = it }

private var _libraryAdd: ImageVector? = null
val Icons.Filled.LibraryAdd: ImageVector
    get() = _libraryAdd ?: materialIcon("Filled.LibraryAdd") {
        materialPath {
            moveTo(4.0f, 6.0f)
            lineTo(2.0f, 6.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            lineTo(4.0f, 20.0f)
            lineTo(4.0f, 6.0f)
            close()
            moveTo(20.0f, 2.0f)
            lineTo(8.0f, 2.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(22.0f, 4.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 11.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-4.0f)
            lineTo(9.0f, 11.0f)
            lineTo(9.0f, 9.0f)
            horizontalLineToRelative(4.0f)
            lineTo(13.0f, 5.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }.also { _libraryAdd = it }

private var _remove: ImageVector? = null
val Icons.Filled.Remove: ImageVector
    get() = _remove ?: materialIcon("Filled.Remove") {
        materialPath {
            moveTo(19.0f, 13.0f)
            horizontalLineTo(5.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }.also { _remove = it }

private var _twoWheeler: ImageVector? = null
val Icons.Filled.TwoWheeler: ImageVector
    get() = _twoWheeler ?: materialIcon("Filled.TwoWheeler") {
        materialPath {
            moveTo(20.0f, 11.0f)
            curveToRelative(-0.18f, 0.0f, -0.36f, 0.03f, -0.53f, 0.05f)
            lineTo(17.41f, 9.0f)
            horizontalLineTo(20.0f)
            verticalLineTo(6.0f)
            lineToRelative(-3.72f, 1.86f)
            lineTo(13.41f, 5.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.59f)
            lineToRelative(2.0f, 2.0f)
            horizontalLineTo(11.0f)
            lineToRelative(-4.0f, 2.0f)
            lineTo(5.0f, 9.0f)
            horizontalLineTo(0.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
            curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
            lineToRelative(2.0f, 2.0f)
            horizontalLineToRelative(3.0f)
            lineToRelative(3.49f, -6.1f)
            lineToRelative(1.01f, 1.01f)
            curveTo(16.59f, 12.64f, 16.0f, 13.75f, 16.0f, 15.0f)
            curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
            curveTo(24.0f, 12.79f, 22.21f, 11.0f, 20.0f, 11.0f)
            close()
            moveTo(4.0f, 17.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
            curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
            curveTo(6.0f, 16.1f, 5.1f, 17.0f, 4.0f, 17.0f)
            close()
            moveTo(20.0f, 17.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
            curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            curveTo(22.0f, 16.1f, 21.1f, 17.0f, 20.0f, 17.0f)
            close()
        }
    }.also { _twoWheeler = it }

private var _uploadFile: ImageVector? = null
val Icons.Filled.UploadFile: ImageVector
    get() = _uploadFile ?: materialIcon("Filled.UploadFile") {
        materialPath {
            moveTo(14.0f, 2.0f)
            lineTo(6.0f, 2.0f)
            curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
            lineTo(4.0f, 20.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
            lineTo(18.0f, 22.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(20.0f, 8.0f)
            lineToRelative(-6.0f, -6.0f)
            close()
            moveTo(18.0f, 20.0f)
            lineTo(6.0f, 20.0f)
            lineTo(6.0f, 4.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(5.0f)
            horizontalLineToRelative(5.0f)
            verticalLineToRelative(11.0f)
            close()
            moveTo(8.0f, 15.01f)
            lineToRelative(1.41f, 1.41f)
            lineTo(11.0f, 14.84f)
            lineTo(11.0f, 19.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.16f)
            lineToRelative(1.59f, 1.59f)
            lineTo(16.0f, 15.01f)
            lineTo(12.01f, 11.0f)
            close()
        }
    }.also { _uploadFile = it }
