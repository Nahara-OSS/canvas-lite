package io.github.naharaoss.canvaslite.ext

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <S : Comparable<S>> slideTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val toLeft = slideInHorizontally(spatialSpec) { x -> -x } togetherWith slideOutHorizontally(spatialSpec) { x -> x }
    val toRight = slideInHorizontally(spatialSpec) { x -> x } togetherWith slideOutHorizontally(spatialSpec) { x -> -x }

    return {
        when {
            initialState >= targetState -> toLeft
            else -> toRight
        }
    }
}