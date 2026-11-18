package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.data.media.formatMediaTime
import com.anacatavc.oneuiextrawidgets.widgets.music.CassetteLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.MiniDiscLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3LayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.VinylLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateCassetteLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateDiscmanLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateIpodLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateMiniDiscLayout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateMp3Layout
import com.anacatavc.oneuiextrawidgets.widgets.music.calculateVinylLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val narrowPillSpec = calculateMp3Layout(widthDp = 190f, heightDp = 80f)
        val stackedSpec = calculateMp3Layout(widthDp = 180f, heightDp = 160f)

        assertEquals(RetroMp3LayoutMode.PILL, pillSpec.mode)
        assertEquals(38f, pillSpec.buttonSizeDp, 0.01f)
        assertEquals(28f, pillSpec.sideButtonSizeDp, 0.01f)
        assertTrue(pillSpec.showUsbCap)
        assertTrue(pillSpec.usbCapWidthDp > 0f)
        assertTrue(pillSpec.showEqualizer)

        assertEquals(RetroMp3LayoutMode.PILL, narrowPillSpec.mode)
        assertFalse(narrowPillSpec.showUsbCap)

        assertEquals(RetroMp3LayoutMode.STACKED, stackedSpec.mode)
        assertTrue(stackedSpec.buttonSizeDp in 36f..46f)
        assertTrue(stackedSpec.buttonSizeDp > stackedSpec.sideButtonSizeDp)
        assertFalse(stackedSpec.showUsbCap)
    }

    @Test
    fun `discman stays circular and scales LCD and ergonomic controls proportionally`() {
        val compactSpec = calculateDiscmanLayout(widthDp = 160f, heightDp = 240f)
        val largeSpec = calculateDiscmanLayout(widthDp = 280f, heightDp = 280f)

        assertEquals(160f, compactSpec.discSizeDp, 0.01f)
        assertEquals(280f, largeSpec.discSizeDp, 0.01f)
        assertTrue(largeSpec.lcdWidthDp > compactSpec.lcdWidthDp)
        assertTrue(largeSpec.lcdHeightDp in 36f..76f)
        assertTrue(largeSpec.buttonSizeDp in 26f..44f)
        assertTrue(largeSpec.buttonSizeDp > largeSpec.sideButtonSizeDp)
    }

    @Test
    fun `formatMediaTime formats unpadded and padded durations accurately`() {
        assertEquals("0:00", formatMediaTime(0L))
        assertEquals("3:05", formatMediaTime(185_000L))
        assertEquals("00:00", formatMediaTime(0L, padMinutes = true))
        assertEquals("03:05", formatMediaTime(185_000L, padMinutes = true))
    }

    @Test
    fun `cassette walkman scales deck proportionally across COMPACT, TALL and WIDE modes`() {
        val compact = calculateCassetteLayout(widthDp = 160f, heightDp = 160f)
        val tall = calculateCassetteLayout(widthDp = 165f, heightDp = 320f)
        val wide = calculateCassetteLayout(widthDp = 330f, heightDp = 160f)

        assertEquals(CassetteLayoutMode.COMPACT, compact.mode)
        assertEquals(CassetteLayoutMode.TALL, tall.mode)
        assertEquals(CassetteLayoutMode.WIDE, wide.mode)
        assertEquals(compact.deckWidthDp * (7f / 12f), compact.deckHeightDp, 0.05f)
        assertTrue(tall.deckWidthDp > compact.deckWidthDp)
        assertTrue(wide.buttonSizeDp > wide.sideButtonSizeDp)
    }

    @Test
    fun `vinyl turntable preserves square platter and ergonomic buttons across modes`() {
        val compact = calculateVinylLayout(widthDp = 160f, heightDp = 160f)
        val tall = calculateVinylLayout(widthDp = 165f, heightDp = 320f)
        val wide = calculateVinylLayout(widthDp = 330f, heightDp = 160f)

        assertEquals(VinylLayoutMode.COMPACT, compact.mode)
        assertEquals(VinylLayoutMode.TALL, tall.mode)
        assertEquals(VinylLayoutMode.WIDE, wide.mode)
        assertTrue(tall.platterSizeDp > compact.platterSizeDp)
        assertTrue(wide.platterSizeDp in 88f..184f)
        assertTrue(compact.buttonSizeDp > compact.sideButtonSizeDp)
    }

    @Test
    fun `minidisc switches cleanly between PILL and STACKED modes`() {
        val widePill = calculateMiniDiscLayout(widthDp = 310f, heightDp = 82f)
        val narrowPill = calculateMiniDiscLayout(widthDp = 190f, heightDp = 82f)
        val stacked = calculateMiniDiscLayout(widthDp = 180f, heightDp = 165f)

        assertEquals(MiniDiscLayoutMode.PILL, widePill.mode)
        assertTrue(widePill.showDiscBadge)
        assertTrue(widePill.showSpectrum)

        assertEquals(MiniDiscLayoutMode.PILL, narrowPill.mode)
        assertFalse(narrowPill.showDiscBadge)
        assertFalse(narrowPill.showSpectrum)

        assertEquals(MiniDiscLayoutMode.STACKED, stacked.mode)
        assertTrue(stacked.showDiscBadge)
        assertTrue(stacked.buttonSizeDp > stacked.sideButtonSizeDp)
    }
}
