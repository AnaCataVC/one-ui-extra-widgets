package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.music.computeEqualizerBarHeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicProgressSpecTest {

    @Test
    fun `paused equalizer returns deterministic resting bars within bounds`() {
        val barsA = computeEqualizerBarHeights(
            positionMs = 12_000L,
            isPlaying = false,
            barCount = 5,
            minHeightDp = 5f,
            maxHeightDp = 18f
        )
        val barsB = computeEqualizerBarHeights(
            positionMs = 45_000L,
            isPlaying = false,
            barCount = 5,
            minHeightDp = 5f,
            maxHeightDp = 18f
        )

        assertEquals(5, barsA.size)
        assertEquals(barsA, barsB)
        assertTrue(barsA.all { it in 5f..18f })
    }

    @Test
    fun `playing equalizer bars move as song position advances`() {
        val t0 = computeEqualizerBarHeights(
            positionMs = 0L,
            isPlaying = true,
            barCount = 5,
            minHeightDp = 4f,
            maxHeightDp = 17f
        )
        val t1 = computeEqualizerBarHeights(
            positionMs = 3_000L,
            isPlaying = true,
            barCount = 5,
            minHeightDp = 4f,
            maxHeightDp = 17f
        )
        val t2 = computeEqualizerBarHeights(
            positionMs = 6_000L,
            isPlaying = true,
            barCount = 5,
            minHeightDp = 4f,
            maxHeightDp = 17f
        )

        assertEquals(5, t0.size)
        assertTrue(t0.all { it in 4f..17f })
        assertTrue(t1.all { it in 4f..17f })
        assertTrue(t2.all { it in 4f..17f })
        assertNotEquals(t0, t1)
        assertNotEquals(t1, t2)
    }
}
