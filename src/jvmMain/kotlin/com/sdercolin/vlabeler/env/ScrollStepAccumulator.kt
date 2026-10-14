package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.model.key.KeySet
import kotlin.math.abs

/**
 * Turns scroll input into discrete steps, so that one notch fires one action on any device.
 *
 * A standard wheel reports one notch (a scroll amount of 1) as a single event, while a precise device or a remote
 * desktop connection reports a notch as a group of small events that arrive in a tight sequence, usually less than a
 * notch in total. The accumulator therefore treats events separated by no more than [GROUP_GAP_MILLIS] as one notch,
 * and asks for one action per group that has moved at least [MIN_NOTCHES]. Within a group only one action can fire.
 *
 * A direction change or an action change also ends the current group, as the user has moved on to something else.
 */
class ScrollStepAccumulator {

    private var pendingKeySet: KeySet? = null
    private var pending = 0f
    private var firedInGroup = false
    private var lastUptimeMillis = 0L

    /**
     * Adds a scroll amount and returns the [KeySet] to fire now, or null if nothing should fire yet.
     */
    fun add(keySet: KeySet, notches: Float, uptimeMillis: Long): KeySet? {
        if (notches == 0f) return null
        val isSameGroup = keySet == pendingKeySet &&
            uptimeMillis - lastUptimeMillis <= GROUP_GAP_MILLIS &&
            pending * notches >= 0f
        if (!isSameGroup) {
            pendingKeySet = keySet
            pending = 0f
            firedInGroup = false
        }
        lastUptimeMillis = uptimeMillis
        pending += notches
        if (firedInGroup || abs(pending) < MIN_NOTCHES) return null
        return fire()
    }

    private fun fire(): KeySet {
        val keySet = pendingKeySet
        pending = 0f
        firedInGroup = true
        return keySet ?: error("No pending key set to fire")
    }

    companion object {
        const val GROUP_GAP_MILLIS = 150L
        const val MIN_NOTCHES = 0.05f
    }
}
