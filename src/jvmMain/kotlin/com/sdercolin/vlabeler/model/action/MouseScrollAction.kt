package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet

/**
 * Actions triggered by mouse wheel scroll. These are fixed presets and cannot be customized.
 *
 * @property keySet The key set that triggers this action.
 */
enum class MouseScrollAction(val keySet: KeySet) {
    GoToNextSample(KeySet(Key.MouseScrollDown, setOf(Key.Alt))),
    GoToPreviousSample(KeySet(Key.MouseScrollUp, setOf(Key.Alt))),
    GoToNextEntry(KeySet(Key.MouseScrollDown, setOf(Key.Ctrl))),
    GoToPreviousEntry(KeySet(Key.MouseScrollUp, setOf(Key.Ctrl))),
    ZoomInCanvas(KeySet(Key.MouseScrollUp)),
    ZoomOutCanvas(KeySet(Key.MouseScrollDown)),
}
