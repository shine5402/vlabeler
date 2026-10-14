package env

import com.sdercolin.vlabeler.env.ScrollStepAccumulator
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrollStepAccumulatorTest {

    private val down = KeySet(Key.MouseScrollDown)
    private val up = KeySet(Key.MouseScrollUp)
    private val downWithCtrl = KeySet(Key.MouseScrollDown, setOf(Key.Ctrl))

    @Test
    fun `a standard notch fires immediately`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 1f, 0))
    }

    @Test
    fun `a notch split into fragments fires once`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 0.1f, 0))
        assertNull(accumulator.add(down, 0.2f, 8))
        assertNull(accumulator.add(down, 0.2f, 16))
    }

    @Test
    fun `two notches fire separately`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 0.1f, 0))
        assertEquals(down, accumulator.add(down, 0.2f, 300))
    }

    @Test
    fun `two fast notches do not fire twice`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 0.1f, 0))
        assertNull(accumulator.add(down, 0.2f, 100))
    }

    @Test
    fun `tiny movements do not fire`() {
        val accumulator = ScrollStepAccumulator()
        assertNull(accumulator.add(down, 0.01f, 0))
    }

    @Test
    fun `direction change fires as a new notch`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 0.2f, 0))
        assertEquals(up, accumulator.add(up, 0.2f, 100))
    }

    @Test
    fun `key set change fires as a new notch`() {
        val accumulator = ScrollStepAccumulator()
        assertEquals(down, accumulator.add(down, 0.2f, 0))
        assertEquals(downWithCtrl, accumulator.add(downWithCtrl, 0.2f, 100))
    }

    @Test
    fun `action name resolves for the fired key set`() {
        assertEquals(MouseScrollAction.GoToNextEntry, MouseScrollAction.getByKeySet(down))
        assertEquals(MouseScrollAction.ZoomOutCanvas, MouseScrollAction.getByKeySet(downWithCtrl))
    }
}
