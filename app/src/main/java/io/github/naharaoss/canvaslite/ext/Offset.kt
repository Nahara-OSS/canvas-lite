package io.github.naharaoss.canvaslite.ext

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2

infix fun Offset.dot(another: Offset) = this.x * another.x + this.y * another.y
val Offset.angle get() = atan2(this.y, this.x) * 180f / PI.toFloat()