package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualitySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkQualitySnapshotTest {

    @Test
    fun `qualityLabel returns bilingual labels for all signal bar levels`() {
        val snapshot = NetworkQualitySnapshot()

        assertEquals("Excelente", snapshot.qualityLabel(bars = 4, isSpanish = true))
        assertEquals("Excellent", snapshot.qualityLabel(bars = 4, isSpanish = false))
        assertEquals("Buena", snapshot.qualityLabel(bars = 3, isSpanish = true))
        assertEquals("Good", snapshot.qualityLabel(bars = 3, isSpanish = false))
        assertEquals("Regular", snapshot.qualityLabel(bars = 2, isSpanish = true))
        assertEquals("Fair", snapshot.qualityLabel(bars = 2, isSpanish = false))
        assertEquals("Débil", snapshot.qualityLabel(bars = 1, isSpanish = true))
        assertEquals("Weak", snapshot.qualityLabel(bars = 1, isSpanish = false))
        assertEquals("Sin señal", snapshot.qualityLabel(bars = 0, isSpanish = true))
        assertEquals("No Signal", snapshot.qualityLabel(bars = 0, isSpanish = false))
    }
}
