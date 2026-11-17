package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.network.NetworkLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.network.calculateNetworkLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLayoutSpecTest {

    @Test
    fun `default 2x2 cell selects SQUARE layout mode with header and extended details`() {
        val spec = calculateNetworkLayout(widthDp = 160f, heightDp = 160f)

        assertEquals(NetworkLayoutMode.SQUARE, spec.mode)
        assertEquals(12f, spec.cardPaddingDp, 0.01f)
        assertTrue(spec.showHeaderTitle)
        assertTrue(spec.showExtendedDetails)
        assertTrue(spec.iconSizeDp in 32f..44f)
    }

    @Test
    fun `compact 2x1 pill selects COMPACT_PILL mode without header`() {
        val spec = calculateNetworkLayout(widthDp = 155f, heightDp = 82f)

        assertEquals(NetworkLayoutMode.COMPACT_PILL, spec.mode)
        assertEquals(10f, spec.cardPaddingDp, 0.01f)
        assertFalse(spec.showHeaderTitle)
        assertFalse(spec.showExtendedDetails)
        assertTrue(spec.iconSizeDp in 24f..36f)
    }

    @Test
    fun `wide 4x1 cell stays in COMPACT_PILL mode with extended details enabled`() {
        val spec = calculateNetworkLayout(widthDp = 300f, heightDp = 82f)

        assertEquals(NetworkLayoutMode.COMPACT_PILL, spec.mode)
        assertFalse(spec.showHeaderTitle)
        assertTrue(spec.showExtendedDetails)
    }

    @Test
    fun `wide 4x2 cell selects EXPANDED mode with larger signal icons`() {
        val squareSpec = calculateNetworkLayout(widthDp = 160f, heightDp = 160f)
        val expandedSpec = calculateNetworkLayout(widthDp = 260f, heightDp = 160f)

        assertEquals(NetworkLayoutMode.EXPANDED, expandedSpec.mode)
        assertEquals(14f, expandedSpec.cardPaddingDp, 0.01f)
        assertTrue(expandedSpec.showHeaderTitle)
        assertTrue(expandedSpec.showExtendedDetails)
        assertTrue(expandedSpec.iconSizeDp in 40f..52f)
        assertTrue(expandedSpec.iconSizeDp >= squareSpec.iconSizeDp)
    }

    @Test
    fun `degenerate zero or negative dimensions coerce safely without invalid sizes`() {
        val zeroSpec = calculateNetworkLayout(widthDp = 0f, heightDp = 0f)
        val negSpec = calculateNetworkLayout(widthDp = -30f, heightDp = -10f)

        assertEquals(NetworkLayoutMode.COMPACT_PILL, zeroSpec.mode)
        assertTrue(zeroSpec.iconSizeDp in 24f..36f)
        assertEquals(NetworkLayoutMode.COMPACT_PILL, negSpec.mode)
        assertTrue(negSpec.iconSizeDp in 24f..36f)
    }
}
