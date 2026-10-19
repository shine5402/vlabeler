package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerCursorState(
    val mouse: Mouse = Mouse.None,
    /**
     * Flattened controllable point index. For multi-entry mode, we have indexes of [-2, 0, 1, 2, 3, ..., -1] for
     * [entries[0].start, entries[0].points[0], entries[0].points[1], entries[0].end, entries[1].points[1], ... ] where
     * the size of points is 2. It's ensured that: entry's end is equal to the next entry's start.
     */
    val pointIndex: Int = NONE_POINT_INDEX,
    /**
     * The position of the point, always paired with [pointIndex].
     */
    val pointPosition: Float? = null,
    val lockedDrag: Boolean = false,
    val previewOnDragging: Boolean = false,
    val forcedDrag: Boolean = false,
    val cascadingDrag: Boolean = false,
    val position: Float? = null,
    /**
     * During dragging, the index offset between the dragging point and the point index.
     */
    val relativeDraggingIndexOffset: Int? = null,
    /**
     * The entries as they were when the current dragging started. Used as the anchor of elastic pushes, so that
     * a point pushed by the dragged one returns to its value at the start of the gesture.
     */
    val preDragEntries: List<EntryInPixel> = listOf(),
) {

    fun startDragging(
        lockedDrag: Boolean,
        withPreview: Boolean,
        forcedDrag: Boolean,
        cascadingDrag: Boolean,
        preDragEntries: List<EntryInPixel> = listOf(),
    ) = copy(
        mouse = Mouse.Dragging,
        lockedDrag = lockedDrag,
        previewOnDragging = withPreview,
        forcedDrag = forcedDrag,
        cascadingDrag = cascadingDrag,
        preDragEntries = preDragEntries,
        relativeDraggingIndexOffset = if (pointPosition != null && position != null) {
            if (position - pointPosition > 0) {
                1
            } else {
                0
            }
        } else {
            null
        },
    )

    fun finishDragging() = copy(
        mouse = Mouse.None,
        lockedDrag = false,
        cascadingDrag = false,
        relativeDraggingIndexOffset = null,
        preDragEntries = listOf(),
    )

    fun moveToNothing() = copy(
        pointIndex = NONE_POINT_INDEX,
        pointPosition = null,
        mouse = Mouse.None,
    )

    fun moveToHover(index: Int, position: Float) = copy(
        pointIndex = index,
        pointPosition = position,
        mouse = Mouse.Hovering,
    )

    enum class Mouse {
        Dragging,
        Hovering,
        None
    }

    val usingStartPoint get() = pointIndex == START_POINT_INDEX
    val usingEndPoint get() = pointIndex == END_POINT_INDEX

    companion object {
        const val NONE_POINT_INDEX = -3
        const val START_POINT_INDEX = -2
        const val END_POINT_INDEX = -1
    }
}
