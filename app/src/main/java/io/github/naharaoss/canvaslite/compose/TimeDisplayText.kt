package io.github.naharaoss.canvaslite.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TimeDisplayText(time: ZonedDateTime, interval: Long = 1000L) {
    var content by remember { mutableStateOf(time.toUserString()) }

    LaunchedEffect(time, interval) {
        while (isActive) {
            content = time.toUserString()
            delay(interval)
        }
    }

    Text(content)
}

private val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

private fun ZonedDateTime.toUserString(now: ZonedDateTime = ZonedDateTime.now()): String {
    val duration = Duration.between(this, now)
    return if (duration.toDays() >= 8) format(formatter) else duration.toUserString
}

private val Duration.toUserString get() = when {
    toDays() >= 2L -> "${toDays()} days ago"
    toDays() == 1L -> "A day ago"
    toHours() >= 2L -> "${toHours()} hours ago"
    toHours() == 1L -> "An hour ago"
    toMinutes() >= 2L -> "${toMinutes()} minutes ago"
    toMinutes() == 1L -> "A minute ago"
    seconds >= 2L -> "$seconds seconds ago"
    seconds == 1L -> "A second ago"
    seconds == 0L -> "Just now"
    else -> "Unknown"
}