@file:OptIn(ExperimentalFoundationApi::class)

package com.example.mynote.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * A custom [OverscrollEffect] that implements a rubber-band bounce effect.
 */
class ElasticOverscrollEffect(
    private val orientation: Orientation = Orientation.Vertical
) : OverscrollEffect {
    val overscrollOffset = Animatable(0f)

    // The node is created once and reused for the lifetime of this effect - it's what actually
    // gets attached into the layout tree by Modifier.overscroll(), so it must share this effect's
    // Animatable rather than own a separate copy of it.
    override val node: Modifier.Node = ElasticOverscrollNode(overscrollOffset, orientation)

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val deltaValue = if (orientation == Orientation.Vertical) delta.y else delta.x
        if (deltaValue == 0f) return performScroll(delta)

        // 1. If we are already in an overscroll state, we might need to reduce it or increase it
        if (abs(overscrollOffset.value) > 0.5f && sign(deltaValue) != sign(overscrollOffset.value)) {
            val newOffset = overscrollOffset.value + deltaValue

            if (sign(newOffset) != sign(overscrollOffset.value)) {
                // The delta is enough to fully cancel the overscroll and then some - snap the
                // rubber band back to rest and hand the remainder to the real scrollable content.
                node.coroutineScope.launch { overscrollOffset.snapTo(0f) }
                val remainderValue = newOffset
                val remainder = if (orientation == Orientation.Vertical) {
                    Offset(0f, remainderValue)
                } else {
                    Offset(remainderValue, 0f)
                }
                return performScroll(remainder) + (delta - remainder)
            } else {
                node.coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                return delta
            }
        }

        // 2. Perform actual scroll
        val consumedByScroll = performScroll(delta)
        val consumedValue = if (orientation == Orientation.Vertical) consumedByScroll.y else consumedByScroll.x
        val deltaLeft = deltaValue - consumedValue

        // 3. If there's delta left and it's from user input, apply rubber banding
        if (source == NestedScrollSource.UserInput && abs(deltaLeft) > 0.5f) {
            val currentOffset = overscrollOffset.value
            val resistance = 1f / (1f + abs(currentOffset) / 500f)
            val addedOffset = deltaLeft * 0.5f * resistance

            node.coroutineScope.launch {
                overscrollOffset.snapTo(currentOffset + addedOffset)
            }
            return delta
        }

        return consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val remaining = performFling(velocity)
        val remainingValue = if (orientation == Orientation.Vertical) remaining.y else remaining.x

        // Only animate if we are already in an overscroll state (e.g. user was dragging).
        // If we hit the edge during a fling from 0, don't trigger the bounce.
        if (abs(overscrollOffset.value) > 0.5f) {
            overscrollOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialVelocity = remainingValue
            )
        } else {
            // Just ensure it's at 0 if we somehow ended up with a tiny offset
            if (overscrollOffset.value != 0f) {
                overscrollOffset.snapTo(0f)
            }
        }
    }

    override val isInProgress: Boolean
        get() = overscrollOffset.value != 0f
}

/**
 * Renders the bounce by offsetting *placement*, not by translating the draw canvas. Compose's
 * own overscroll samples use a [LayoutModifierNode] for exactly this reason: shifting where the
 * already-measured content is *placed* reliably moves it on screen regardless of what clipping
 * or drawing happens elsewhere in the surrounding modifier chain, which a plain draw-time canvas
 * translate cannot guarantee.
 */
private class ElasticOverscrollNode(
    private val overscrollOffset: Animatable<Float, *>,
    private val orientation: Orientation
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Reading .value here (during placement) is what makes this node automatically
            // re-place itself whenever the Animatable's value changes.
            val offset = overscrollOffset.value.roundToInt()
            if (orientation == Orientation.Vertical) {
                placeable.placeRelativeWithLayer(0, offset)
            } else {
                placeable.placeRelativeWithLayer(offset, 0)
            }
        }
    }
}

/**
 * A factory that creates [ElasticOverscrollEffect] instances.
 */
@ExperimentalFoundationApi
object ElasticOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect {
        return ElasticOverscrollEffect()
    }

    override fun hashCode(): Int = System.identityHashCode(this)
    override fun equals(other: Any?): Boolean = this === other
}

@Composable
fun rememberElasticOverscrollEffect(
    orientation: Orientation = Orientation.Vertical
): ElasticOverscrollEffect {
    return remember(orientation) {
        ElasticOverscrollEffect(orientation)
    }
}

fun Modifier.elasticOverscroll(effect: ElasticOverscrollEffect): Modifier = this.overscroll(effect)
