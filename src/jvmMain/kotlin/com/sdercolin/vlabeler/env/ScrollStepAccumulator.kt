package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.model.action.MouseScrollAction
import kotlin.math.abs
import kotlin.math.sign

/**
 * Turns scroll amounts into discrete steps, so that precise devices (touchpads, high-resolution wheels) reporting
 * fractions of a notch do not trigger one action per tiny movement.
 *
 * One step is one notch (a scroll amount of 1). Partial notches are kept until they add up. They are dropped when the
 * direction or the action changes, or when the input pauses for longer than [idleTimeoutMillis].
 *
 * @property idleTimeoutMillis The pause after which the accumulated partial notch is dropped.
 */
class ScrollStepAccumulator(private val idleTimeoutMillis: Long = DEFAULT_IDLE_TIMEOUT_MILLIS) {

    private var pending = 0f
    private var lastAction: MouseScrollAction? = null
    private var lastUptimeMillis = 0L

    /**
     * Adds a scroll amount in notches and returns how many steps of [action] it completes.
     */
    fun add(action: MouseScrollAction, notches: Float, uptimeMillis: Long): Int {
        val isContinuous = action == lastAction &&
            uptimeMillis - lastUptimeMillis <= idleTimeoutMillis &&
            pending * notches >= 0f
        if (!isContinuous) pending = 0f
        lastAction = action
        lastUptimeMillis = uptimeMillis
        pending += notches
        val steps = abs(pending).toInt()
        pending -= steps * pending.sign
        return steps
    }

    companion object {
        const val DEFAULT_IDLE_TIMEOUT_MILLIS = 400L
    }
}
