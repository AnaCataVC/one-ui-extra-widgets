package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.music.computeEqualizerBarHeights
import com.anacatavc.oneuiextrawidgets.widgets.music.computePlaybackRotationPhaseDeg
import com.anacatavc.oneuiextrawidgets.widgets.music.computeVuMeterActiveSegments
import com.anacatavc.oneuiextrawidgets.widgets.music.quantizeProgressForCanvas
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

    @Test
    fun `quantizeProgressForCanvas buckets nearby values and clamps bounds`() {
        assertEquals(0f, quantizeProgressForCanvas(-0.2f), 0.0001f)
        assertEquals(1f, quantizeProgressForCanvas(1.4f), 0.0001f)
        assertEquals(0.25f, quantizeProgressForCanvas(0.245f), 0.0001f)
        assertEquals(0.25f, quantizeProgressForCanvas(0.255f), 0.0001f)
        assertEquals(0.50f, quantizeProgressForCanvas(0.495f), 0.0001f)
    }

    @Test
    fun `computePlaybackRotationPhaseDeg advances continuously with playback position`() {
        val idleZero = computePlaybackRotationPhaseDeg(positionMs = 0L, isPlaying = false)
        val step1 = computePlaybackRotationPhaseDeg(positionMs = 2_000L, isPlaying = true)
        val step2 = computePlaybackRotationPhaseDeg(positionMs = 4_000L, isPlaying = true)
        val step3 = computePlaybackRotationPhaseDeg(positionMs = 6_000L, isPlaying = true)

        assertEquals(0f, idleZero, 0.001f)
        assertTrue(step1 in 0f..360f)
        assertTrue(step2 in 0f..360f)
        assertTrue(step3 in 0f..360f)
        assertNotEquals(step1, step2)
        assertNotEquals(step2, step3)
    }

    @Test
    fun `computeVuMeterActiveSegments stays at 1 when paused and oscillates when playing`() {
        val pausedA = computeVuMeterActiveSegments(positionMs = 1_000L, isPlaying = false, segmentCount = 5)
        val pausedB = computeVuMeterActiveSegments(positionMs = 9_000L, isPlaying = false, segmentCount = 5)
        val samples = (0..10).map { idx ->
            computeVuMeterActiveSegments(positionMs = idx * 1_500L, isPlaying = true, segmentCount = 5)
        }

        assertEquals(1, pausedA)
        assertEquals(1, pausedB)
        assertTrue(samples.all { it in 2..5 })
        assertTrue(samples.toSet().size >= 2)
    }
}
