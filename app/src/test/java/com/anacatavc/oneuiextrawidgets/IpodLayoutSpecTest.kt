package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.music.IpodLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3LayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateDiscmanLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateIpodLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateMp3Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpodLayoutSpecTest {

    @Test
    fun `compact 2x2 widget uses COMPACT mode and fits wheel`() {
        val spec = calculateIpodLayout(widthDp = 160f, heightDp = 160f)

        assertEquals(IpodLayoutMode.COMPACT, spec.mode)
        assertTrue(spec.wheelSizeDp in 72f..128f)
    }

    @Test
    fun `elongated vertical widget switches to TALL mode and scales wheel up`() {
        val compactSpec = calculateIpodLayout(widthDp = 165f, heightDp = 165f)
        val tallSpec = calculateIpodLayout(widthDp = 165f, heightDp = 340f)

        assertEquals(IpodLayoutMode.TALL, tallSpec.mode)
        assertTrue(tallSpec.wheelSizeDp > compactSpec.wheelSizeDp)
        assertTrue(tallSpec.lcdHeightDp in 68f..136f)
        assertTrue(tallSpec.lcdHeightDp + tallSpec.wheelSizeDp < 340f)
    }

    @Test
    fun `wide horizontal widget switches to WIDE mode for side-by-side layout`() {
        val wideSpec = calculateIpodLayout(widthDp = 330f, heightDp = 160f)

        assertEquals(IpodLayoutMode.WIDE, wideSpec.mode)
        assertTrue(wideSpec.wheelSizeDp in 88f..180f)
        assertTrue(wideSpec.wheelSizeDp <= 160f)
    }

    @Test
    fun `retro mp3 switches between PILL and STACKED layouts cleanly`() {
        val pillSpec = calculateMp3Layout(widthDp = 300f, heightDp = 80f)
        val stackedSpec = calculateMp3Layout(widthDp = 180f, heightDp = 160f)

        assertEquals(RetroMp3LayoutMode.PILL, pillSpec.mode)
        assertEquals(34f, pillSpec.buttonSizeDp, 0.01f)
        assertEquals(RetroMp3LayoutMode.STACKED, stackedSpec.mode)
        assertTrue(stackedSpec.buttonSizeDp in 34f..44f)
    }

    @Test
    fun `discman stays circular and scales central pill proportionally`() {
        val compactSpec = calculateDiscmanLayout(widthDp = 160f, heightDp = 240f)
        val largeSpec = calculateDiscmanLayout(widthDp = 280f, heightDp = 280f)

        assertEquals(160f, compactSpec.discSizeDp, 0.01f)
        assertEquals(280f, largeSpec.discSizeDp, 0.01f)
        assertTrue(largeSpec.pillWidthDp > compactSpec.pillWidthDp)
        assertTrue(largeSpec.pillHeightDp in 36f..72f)
        assertTrue(largeSpec.buttonSizeDp in 22f..36f)
    }
}
