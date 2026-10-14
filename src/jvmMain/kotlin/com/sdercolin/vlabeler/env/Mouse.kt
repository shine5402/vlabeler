@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import com.sdercolin.vlabeler.model.key.Key

/**
 * Whether this [PointerEvent] is a horizontal scroll, which Compose reports as a non-zero x delta.
 */
val PointerEvent.isHorizontalScroll: Boolean
    get() {
        val change = changes.firstOrNull()?.scrollDelta ?: return false
        return change.x != 0f && change.y == 0f
    }

/**
 * The scroll amount of this [PointerEvent] in wheel notches, on the same axis that [toVirtualKey] uses.
 */
val PointerEvent.scrollNotches: Float
    get() {
        val change = changes.firstOrNull()?.scrollDelta ?: return 0f
        return if (change.x != 0f) change.x else change.y
    }

/**
 * Converts a [PointerEvent] to a [Key] if it is a mouse click or scroll event.
 */
fun PointerEvent.toVirtualKey(): Key? {
    return when (type) {
        PointerEventType.Press, PointerEventType.Release -> when (button) {
            PointerButton.Primary -> {
                Key.MouseLeftClick
            }
            PointerButton.Secondary -> {
                Key.MouseRightClick
            }
            else -> null
        }
        PointerEventType.Scroll -> {
            val change = changes.firstOrNull()?.scrollDelta ?: return null
            if (change.x != 0f && change.y != 0f) {
                Log.debug("Warning: getting a mouse scroll event with both x:${change.x} and y:${change.y} changes")
            }
            when {
                change.x > 0 -> {
                    Key.MouseScrollDown
                }
                change.x < 0 -> {
                    Key.MouseScrollUp
                }
                change.y > 0 -> {
                    Key.MouseScrollDown
                }
                change.y < 0 -> {
                    Key.MouseScrollUp
                }
                else -> null
            }
        }
        else -> null
    }
}
