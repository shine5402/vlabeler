package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.injectLabelerParams
import com.sdercolin.vlabeler.util.toParamMap
import org.junit.jupiter.api.Test
import testutil.TestEnv
import testutil.TestLabelers
import kotlin.test.assertEquals

/**
 * Tests for [MarkerState.getDraggedEntries], the core drag-constraint logic.
 *
 * All states use sampleRate = 1000 Hz / resolution = 1, so 1 ms == 1 px and the canvas is 1000 px long.
 *
 * Continuous scenario (nnsvs-singer, no fields): entries at 100..200, 200..350, 350..500 with flattened point indexes
 * -2 (start), 0 (border at 200), 1 (border at 350), -1 (end).
 *
 * Non-continuous scenario (utau-singer): 1 entry with start = 100, end = 600, points = [fixed = 400, preu = 250,
 * ovl = 150, left = 100]. "left" (index 3) replaces start, so the labeler uses an implicit start. Connected
 * constraints (a <= b): (left, fixed), (left, preu), (preu, fixed), (ovl, fixed).
 */
class MarkerStateDragTest {

    private val start = MarkerCursorState.START_POINT_INDEX
    private val end = MarkerCursorState.END_POINT_INDEX

    private fun continuousState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
            MarkerStateFactory.entry(350f, 500f, name = "c"),
        ),
    )

    private fun continuousStateEditingMiddleEntry() = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
            MarkerStateFactory.entry(350f, 500f, name = "c"),
        ),
        editedIndexes = listOf(1),
    )

    private fun utauSingerState(labelerConf: LabelerConf = TestLabelers.utauSinger) = MarkerStateFactory.create(
        labelerConf = labelerConf,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f)),
        ),
    )

    private fun utauSingerConfWithParams(params: Map<String, Any>): LabelerConf {
        TestEnv.ensureLogDirectory()
        val base = TestLabelers.utauSinger
        val fullParams = (base.getDefaultParams() + params).toParamMap()
        return base.injectLabelerParams(fullParams)
    }

    private fun MarkerState.entry(index: Int) = entriesInPixel[index]

    // region continuous mode

    @Test
    fun nonePointIndexReturnsEntriesUnchanged() {
        val state = continuousState()
        assertEquals(state.entriesInPixel, state.getDraggedEntries(MarkerCursorState.NONE_POINT_INDEX, 250f, false))
    }

    @Test
    fun draggingStartMovesFirstEntryStart() {
        val state = continuousState()
        val result = state.getDraggedEntries(start, 150f, forcedDrag = false)
        assertEquals(
            listOf(state.entry(0).copy(start = 150f), state.entry(1), state.entry(2)),
            result,
        )
    }

    @Test
    fun draggingStartIsClampedByFirstMiddlePoint() {
        val state = continuousState()
        // the first border (200) is the first middle point, so start cannot pass it
        val result = state.getDraggedEntries(start, 250f, forcedDrag = false)
        assertEquals(200f, result[0].start)
    }

    @Test
    fun draggingStartIsClampedByLeftBorder() {
        val state = continuousState()
        val result = state.getDraggedEntries(start, -50f, forcedDrag = false)
        assertEquals(0f, result[0].start)
    }

    @Test
    fun draggingEndMovesLastEntryEnd() {
        val state = continuousState()
        val result = state.getDraggedEntries(end, 400f, forcedDrag = false)
        assertEquals(
            listOf(state.entry(0), state.entry(1), state.entry(2).copy(end = 400f)),
            result,
        )
    }

    @Test
    fun draggingEndIsClampedByLastMiddlePoint() {
        val state = continuousState()
        // the last border (350) is the last middle point, so end cannot pass it
        val result = state.getDraggedEntries(end, 300f, forcedDrag = false)
        assertEquals(350f, result[2].end)
    }

    @Test
    fun draggingEndIsClampedByRightBorder() {
        val state = continuousState()
        // right border is the canvas width (1000); max is rightBorder - 1
        val result = state.getDraggedEntries(end, 1500f, forcedDrag = false)
        assertEquals(999f, result[2].end)
    }

    @Test
    fun draggingBorderMovesBothAdjacentEntries() {
        val state = continuousState()
        val result = state.getDraggedEntries(0, 250f, forcedDrag = false)
        assertEquals(
            listOf(
                state.entry(0).copy(end = 250f),
                state.entry(1).copy(start = 250f),
                state.entry(2),
            ),
            result,
        )
    }

    @Test
    fun draggingBorderIsClampedByLeftEntryStart() {
        val state = continuousState()
        // the left entry (100..200) has no points, so its start (100) is the min
        val result = state.getDraggedEntries(0, 50f, forcedDrag = false)
        assertEquals(100f, result[0].end)
        assertEquals(100f, result[1].start)
    }

    @Test
    fun draggingBorderIsClampedByRightEntryEnd() {
        val state = continuousState()
        // the right entry (200..350) has no points, so its end (350) is the max
        val result = state.getDraggedEntries(0, 400f, forcedDrag = false)
        assertEquals(350f, result[0].end)
        assertEquals(350f, result[1].start)
    }

    @Test
    fun forcedBorderDragPushesFollowingEntries() {
        val state = continuousState()
        // border 0 is dragged to 400, past the end (350) of entry "b":
        // "a" is extended to 100..400; "b" is squeezed to 400..400; "c" is pushed to 400..500
        val result = state.getDraggedEntries(0, 400f, forcedDrag = true)
        assertEquals(
            listOf(
                state.entry(0).copy(end = 400f),
                state.entry(1).copy(start = 400f, end = 400f),
                state.entry(2).copy(start = 400f),
            ),
            result,
        )
    }

    @Test
    fun forcedStartDragPushesFollowingEntries() {
        val state = continuousState()
        // start is dragged to 250, past the border at 200:
        // "a" collapses to 250..250; "b" is squeezed to 250..350; "c" is untouched
        val result = state.getDraggedEntries(start, 250f, forcedDrag = true)
        assertEquals(
            listOf(
                state.entry(0).copy(start = 250f, end = 250f),
                state.entry(1).copy(start = 250f),
                state.entry(2),
            ),
            result,
        )
    }

    @Test
    fun forcedEndDragPushesPrecedingEntries() {
        val state = continuousState()
        // end is dragged to 300, past the border at 350:
        // "c" collapses to 300..300; "b" is squeezed to 200..300; "a" is untouched
        val result = state.getDraggedEntries(end, 300f, forcedDrag = true)
        assertEquals(
            listOf(
                state.entry(0),
                state.entry(1).copy(end = 300f),
                state.entry(2).copy(start = 300f, end = 300f),
            ),
            result,
        )
    }

    @Test
    fun draggingStartOfMiddleEntryIsClampedByPreviousEntryStart() {
        val state = continuousStateEditingMiddleEntry()
        // editing only entry "b" (200..350): leftBorder is the previous entry's start (100)
        val moved = state.getDraggedEntries(start, 150f, forcedDrag = false)
        assertEquals(150f, moved[0].start)
        val clamped = state.getDraggedEntries(start, 50f, forcedDrag = false)
        assertEquals(100f, clamped[0].start)
    }

    @Test
    fun draggingEndOfMiddleEntryIsClampedByNextEntryEnd() {
        val state = continuousStateEditingMiddleEntry()
        // editing only entry "b" (200..350): rightBorder is the next entry's end (500)
        val moved = state.getDraggedEntries(end, 400f, forcedDrag = false)
        assertEquals(400f, moved[0].end)
        val clamped = state.getDraggedEntries(end, 520f, forcedDrag = false)
        assertEquals(499f, clamped[0].end)
    }

    // endregion

    // region utau-singer (implicit start + field constraints)

    @Test
    fun draggingImplicitStartWritesReplaceStartField() {
        val state = utauSingerState()
        // dragging start moves the "left" point (points[3]); the raw start is re-synced to the minimum point
        val result = state.getDraggedEntries(start, 200f, forcedDrag = false)
        assertEquals(
            state.entry(0).copy(start = 150f, points = listOf(400f, 250f, 150f, 200f)),
            result[0],
        )
    }

    @Test
    fun draggingImplicitStartIsClampedByConstrainedFields() {
        val state = utauSingerState()
        // "left" <= "fixed" (400) and "left" <= "preu" (250), so the max is 250
        val result = state.getDraggedEntries(start, 300f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 150f, 250f), result[0].points)
        assertEquals(150f, result[0].start)
    }

    @Test
    fun draggingImplicitStartIsClampedByLeftBorder() {
        val state = utauSingerState()
        val result = state.getDraggedEntries(start, -50f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 150f, 0f), result[0].points)
        assertEquals(0f, result[0].start)
    }

    @Test
    fun draggingFieldPointIsClampedByConstraintsOnBothSides() {
        val state = utauSingerState()
        // "preu" (index 1): "left" (100) <= "preu" <= "fixed" (400)
        val moved = state.getDraggedEntries(1, 350f, forcedDrag = false)
        assertEquals(listOf(400f, 350f, 150f, 100f), moved[0].points)
        val clampedLow = state.getDraggedEntries(1, 50f, forcedDrag = false)
        assertEquals(listOf(400f, 100f, 150f, 100f), clampedLow[0].points)
        val clampedHigh = state.getDraggedEntries(1, 450f, forcedDrag = false)
        assertEquals(listOf(400f, 400f, 150f, 100f), clampedHigh[0].points)
    }

    @Test
    fun draggingUnconstrainedFieldPointBelowImplicitStartLowersStart() {
        val state = utauSingerState()
        // "ovl" (index 2) has no lower constraint; with an implicit start its min is the left canvas border,
        // and dragging it below "left" re-syncs the raw start to the new minimum point
        val result = state.getDraggedEntries(2, 20f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 20f, 100f), result[0].points)
        assertEquals(20f, result[0].start)
    }

    @Test
    fun draggingFieldPointIsClampedByMaximumOfConstrainingPoints() {
        val state = utauSingerState()
        // "fixed" (index 0) must be >= "left" (100), "preu" (250) and "ovl" (150): min is 250
        val result = state.getDraggedEntries(0, 200f, forcedDrag = false)
        assertEquals(listOf(250f, 250f, 150f, 100f), result[0].points)
    }

    @Test
    fun draggingFieldPointWithoutUpperConstraintIsClampedByEntryEndWhenPushDisabled() {
        val noPushConf = TestLabelers.utauSinger.let { conf ->
            val fixed = conf.fields[0].copy(
                constraints = listOf(LabelerConf.Constraint(min = 3)),
            )
            conf.copy(fields = listOf(fixed) + conf.fields.drop(1))
        }
        val state = utauSingerState(noPushConf)
        // "fixed" has no upper field constraint and pushMax is false, so the max is end (600)
        val result = state.getDraggedEntries(0, 700f, forcedDrag = false)
        assertEquals(listOf(600f, 250f, 150f, 100f), result[0].points)
    }

    @Test
    fun draggingEndIsClampedByLastMiddlePointWhenPushDisabled() {
        val noPushConf = TestLabelers.utauSinger.let { conf ->
            val fixed = conf.fields[0].copy(
                constraints = listOf(LabelerConf.Constraint(min = 3)),
            )
            conf.copy(fields = listOf(fixed) + conf.fields.drop(1))
        }
        val state = utauSingerState(noPushConf)
        val moved = state.getDraggedEntries(end, 550f, forcedDrag = false)
        assertEquals(550f, moved[0].end)
        // "fixed" (400) is the largest middle point and pushedByMax is false
        val clamped = state.getDraggedEntries(end, 300f, forcedDrag = false)
        assertEquals(400f, clamped[0].end)
    }

    @Test
    fun draggingFixedPastEndPushesEndAndRestoresOnDragBackByDefault() {
        val state = utauSingerState()
        // "fixed" (400) is dragged past "end" (600) to 700: both move to 700
        val pushed = state.getDraggedEntries(0, 700f, forcedDrag = false)
        assertEquals(listOf(700f, 250f, 150f, 100f), pushed[0].points)
        assertEquals(700f, pushed[0].end)

        // dragging back to 650 within the same drag brings "end" back to 650
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val pulledBack = state2.getDraggedEntries(0, 650f, forcedDrag = false)
        assertEquals(listOf(650f, 250f, 150f, 100f), pulledBack[0].points)
        assertEquals(650f, pulledBack[0].end)

        // dragging back below the original "end" (600) stops "end" at 600
        val state3 = MarkerStateFactory.continueDrag(state, pulledBack)
        val restored = state3.getDraggedEntries(0, 500f, forcedDrag = false)
        assertEquals(listOf(500f, 250f, 150f, 100f), restored[0].points)
        assertEquals(600f, restored[0].end)
    }

    @Test
    fun draggingFixedPastEndKeepsPushedEndWhenStickyEnabled() {
        val conf = utauSingerConfWithParams(mapOf("keepPushedFixedAndRight" to true))
        val state = utauSingerState(conf)
        val pushed = state.getDraggedEntries(0, 700f, forcedDrag = false)
        assertEquals(listOf(700f, 250f, 150f, 100f), pushed[0].points)
        assertEquals(700f, pushed[0].end)

        // dragging "fixed" back to 500 keeps "end" at the pushed position (700)
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val draggedBack = state2.getDraggedEntries(0, 500f, forcedDrag = false)
        assertEquals(listOf(500f, 250f, 150f, 100f), draggedBack[0].points)
        assertEquals(700f, draggedBack[0].end)
    }

    @Test
    fun draggingEndPastFixedPushesFixedAndClampsAtPreu() {
        val state = utauSingerState()
        // "end" (600) is dragged left past "fixed" (400) to 300: both move to 300
        val pushed = state.getDraggedEntries(end, 300f, forcedDrag = false)
        assertEquals(300f, pushed[0].end)
        assertEquals(listOf(300f, 250f, 150f, 100f), pushed[0].points)

        // dragging further left to 200 is clamped by "preu" (250)
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val clamped = state2.getDraggedEntries(end, 200f, forcedDrag = false)
        assertEquals(250f, clamped[0].end)
        assertEquals(listOf(250f, 250f, 150f, 100f), clamped[0].points)

        // dragging back right to 350 brings "fixed" back to 350, and to 500 restores "fixed" to 400
        val state3 = MarkerStateFactory.continueDrag(state, clamped)
        val partialRestore = state3.getDraggedEntries(end, 350f, forcedDrag = false)
        assertEquals(350f, partialRestore[0].end)
        assertEquals(listOf(350f, 250f, 150f, 100f), partialRestore[0].points)

        val state4 = MarkerStateFactory.continueDrag(state, partialRestore)
        val fullRestore = state4.getDraggedEntries(end, 500f, forcedDrag = false)
        assertEquals(500f, fullRestore[0].end)
        assertEquals(listOf(400f, 250f, 150f, 100f), fullRestore[0].points)
    }

    @Test
    fun draggingEndPastFixedKeepsPushedFixedWhenStickyEnabled() {
        val conf = utauSingerConfWithParams(mapOf("keepPushedFixedAndRight" to true))
        val state = utauSingerState(conf)
        val pushed = state.getDraggedEntries(end, 300f, forcedDrag = false)
        assertEquals(300f, pushed[0].end)
        assertEquals(listOf(300f, 250f, 150f, 100f), pushed[0].points)

        // dragging "end" back right to 500 keeps "fixed" at 300
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val draggedBack = state2.getDraggedEntries(end, 500f, forcedDrag = false)
        assertEquals(500f, draggedBack[0].end)
        assertEquals(listOf(300f, 250f, 150f, 100f), draggedBack[0].points)
    }

    @Test
    fun draggingLeftWhenNegativeOvlDisabledPushesOvlAndRestoresOnDragBack() {
        val conf = utauSingerConfWithParams(mapOf("useNegativeOvl" to false))
        val state = utauSingerState(conf)
        // "left" (100) is dragged past "ovl" (150) to 200: both move to 200
        val pushed = state.getDraggedEntries(start, 200f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 200f, 200f), pushed[0].points)
        assertEquals(200f, pushed[0].start)

        // dragging further right to 300 is clamped by "preu" (250)
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val clamped = state2.getDraggedEntries(start, 300f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 250f, 250f), clamped[0].points)
        assertEquals(250f, clamped[0].start)

        // dragging back left to 180 brings "ovl" back to 180, and to 120 restores "ovl" to 150
        val state3 = MarkerStateFactory.continueDrag(state, clamped)
        val partialRestore = state3.getDraggedEntries(start, 180f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 180f, 180f), partialRestore[0].points)
        assertEquals(180f, partialRestore[0].start)

        val state4 = MarkerStateFactory.continueDrag(state, partialRestore)
        val fullRestore = state4.getDraggedEntries(start, 120f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 150f, 120f), fullRestore[0].points)
        assertEquals(120f, fullRestore[0].start)

        // dragging "ovl" (index 2) to the left of "left" (100) is still clamped at "left" (does not push "left")
        val ovlClamped = state.getDraggedEntries(2, 50f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 100f, 100f), ovlClamped[0].points)
    }

    @Test
    fun draggingLeftWhenNegativeOvlDisabledKeepsPushedOvlWhenStickyEnabled() {
        val conf = utauSingerConfWithParams(
            mapOf("useNegativeOvl" to false, "keepPushedOverlap" to true),
        )
        val state = utauSingerState(conf)
        val pushed = state.getDraggedEntries(start, 200f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 200f, 200f), pushed[0].points)

        // dragging "left" back to 120 keeps "ovl" at 200
        val state2 = MarkerStateFactory.continueDrag(state, pushed)
        val draggedBack = state2.getDraggedEntries(start, 120f, forcedDrag = false)
        assertEquals(listOf(400f, 250f, 200f, 120f), draggedBack[0].points)
        assertEquals(120f, draggedBack[0].start)
    }

    @Test
    fun forcedFieldDragPastEndExtendsEnd() {
        val state = utauSingerState()
        // forced drag ignores constraints: "fixed" goes to 700, and the entry end is extended to follow it
        val result = state.getDraggedEntries(0, 700f, forcedDrag = true)
        assertEquals(
            state.entry(0).copy(end = 700f, points = listOf(700f, 250f, 150f, 100f)),
            result[0],
        )
    }

    @Test
    fun forcedFieldDragPushesConstrainedPointsAlong() {
        val state = utauSingerState()
        // "fixed" is force-dragged to 120; "preu" (250) and "ovl" (150) must stay <= "fixed" so they are pushed
        // down to 120, while "left" (100) is already below and stays
        val result = state.getDraggedEntries(0, 120f, forcedDrag = true)
        assertEquals(
            state.entry(0).copy(points = listOf(120f, 120f, 120f, 100f)),
            result[0],
        )
    }

    @Test
    fun forcedEndDragBeforeStartCollapsesWholeEntry() {
        val state = utauSingerState()
        // end is force-dragged to 90, before every point: the entry collapses to a zero-length entry at 90
        val result = state.getDraggedEntries(end, 90f, forcedDrag = true)
        assertEquals(
            state.entry(0).copy(start = 90f, end = 90f, points = listOf(90f, 90f, 90f, 90f)),
            result[0],
        )
    }

    // endregion
}
