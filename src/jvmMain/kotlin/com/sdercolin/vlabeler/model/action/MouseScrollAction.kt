package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet

/**
 * Actions triggered by mouse wheel scroll. These are fixed presets and cannot be customized.
 *
 * @property keySet The key set that triggers this action.
 */
enum class MouseScrollAction(val keySet: KeySet) {
    GoToNextSample(KeySet(Key.MouseScrollDown, setOf(Key.Shift, Key.Ctrl))),
    GoToPreviousSample(KeySet(Key.MouseScrollUp, setOf(Key.Shift, Key.Ctrl))),
    GoToNextEntry(KeySet(Key.MouseScrollDown, setOf(Key.Shift))),
    GoToPreviousEntry(KeySet(Key.MouseScrollUp, setOf(Key.Shift))),
    ZoomInCanvas(KeySet(Key.MouseScrollUp)),
    ZoomOutCanvas(KeySet(Key.MouseScrollDown)),
}
