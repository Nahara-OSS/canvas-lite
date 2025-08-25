package io.github.naharaoss.canvaslite.compose

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import io.github.naharaoss.canvaslite.engine.param.CurvePoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun CurveEditor(
    modifier: Modifier = Modifier,
    points: List<CurvePoint>,
    selected: CurvePoint? = null,
    onSelect: (CurvePoint) -> Unit,
    onAdd: (CurvePoint) -> Unit,
    onEdit: (CurvePoint, CurvePoint) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.secondaryContainer
    val radius = 48f
    val radiusSqr = radius * radius

    fun DrawScope.offsetOf(point: CurvePoint) = Offset(
        x = point.input * (size.width - radius * 2) + radius,
        y = (1f - point.output) * (size.height - radius * 2) + radius
    )

    fun AwaitPointerEventScope.offsetOf(point: CurvePoint) = Offset(
        x = point.input * (size.width - radius * 2) + radius,
        y = (1f - point.output) * (size.height - radius * 2) + radius
    )

    fun AwaitPointerEventScope.curveOf(offset: Offset) = CurvePoint(
        input = min(max((offset.x - radius) / (size.width - radius * 2), 0f), 1f),
        output = min(max(1f - (offset.y - radius) / (size.height - radius * 2), 0f), 1f)
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                val pointer = awaitFirstDown()
                var editingPoint: CurvePoint? = null

                for (point in points) {
                    val position = offsetOf(point)
                    Log.d("CurveEditor", "$point")

                    if ((pointer.position - position).getDistanceSquared() <= radiusSqr) {
                        onSelect(point)
                        editingPoint = point
                        break
                    }
                }

                if (editingPoint == null) {
                    editingPoint = curveOf(pointer.position)
                    onAdd(editingPoint)
                }

                pointer.consume()

                drag(pointer.id) { pointer ->
                    val nextPoint = curveOf(pointer.position)
                    onEdit(editingPoint!!, nextPoint)
                    pointer.consume()
                    editingPoint = nextPoint
                }
            }
        }) {
            for (i in 0..4) {
                drawLine(
                    color = gridColor,
                    start = Offset(radius, radius + ((size.height - radius * 2) * i / 4)),
                    end = Offset(size.width - radius, radius + ((size.height - radius * 2) * i / 4)),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gridColor,
                    start = Offset(radius + ((size.width - radius * 2) * i / 4), radius),
                    end = Offset(radius + ((size.width - radius * 2) * i / 4), size.height - radius),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            for (i in 0..(points.size - 2)) {
                drawLine(
                    color = primaryColor,
                    start = offsetOf(points[i]),
                    end = offsetOf(points[i + 1]),
                    strokeWidth = 16f,
                    cap = StrokeCap.Round
                )
            }

            for (point in points) {
                if (selected == point) {
                    drawCircle(
                        color = primaryColor,
                        center = offsetOf(point),
                        radius = radius
                    )
                } else {
                    drawCircle(
                        color = primaryColor,
                        center = offsetOf(point),
                        radius = radius - 8f,
                        style = Stroke(width = 16f)
                    )
                }
            }
        }
    }
}