package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayAnimationQualityTest {
    @Test
    fun lowDetailMatchesTheCurrentAnimationBaseline() {
        val detail = BirthdayAnimationQuality.Low.detail

        assertEquals(30, detail.nightStarCount)
        assertEquals(34, detail.passingMeteorTrailCount)
        assertEquals(24, detail.giantMeteorTrailCount)
        assertEquals(15, detail.meteorImpactDropCount)
        assertEquals(11, detail.fishSplashDropCount)
        assertEquals(3, detail.pondRippleCount)
        assertEquals(9, detail.fishSegmentCount)
        assertEquals(0, detail.giftLightParticleCount)
    }

    @Test
    fun selectorKeepsEmulatorsAndLowRamDevicesOnLowQuality() {
        val quality =
            BirthdayAnimationQualitySelector.select(
                BirthdayAnimationDeviceProfile(
                    isLowRamDevice = false,
                    totalMemoryMb = 16 * 1024L,
                    memoryClassMb = 512,
                    cpuCoreCount = 12,
                    displayPixelCount = 2_000_000,
                    isEmulator = true,
                ),
            )

        assertEquals(BirthdayAnimationQuality.Low, quality)
    }

    @Test
    fun selectorChoosesStandardForTypicalModernPhone() {
        val quality =
            BirthdayAnimationQualitySelector.select(
                BirthdayAnimationDeviceProfile(
                    isLowRamDevice = false,
                    totalMemoryMb = 6 * 1024L,
                    memoryClassMb = 256,
                    cpuCoreCount = 8,
                    displayPixelCount = 2_600_000,
                    isEmulator = false,
                ),
            )

        assertEquals(BirthdayAnimationQuality.Standard, quality)
    }

    @Test
    fun selectorChoosesRefinedForHighMemoryPhone() {
        val quality =
            BirthdayAnimationQualitySelector.select(
                BirthdayAnimationDeviceProfile(
                    isLowRamDevice = false,
                    totalMemoryMb = 12 * 1024L,
                    memoryClassMb = 512,
                    cpuCoreCount = 12,
                    displayPixelCount = 3_000_000,
                    isEmulator = false,
                ),
            )

        assertEquals(BirthdayAnimationQuality.Refined, quality)
    }

    @Test
    fun qualityCanBeLoweredOneStepWithoutSkippingStandard() {
        assertEquals(BirthdayAnimationQuality.Standard, BirthdayAnimationQuality.Refined.lower())
        assertEquals(BirthdayAnimationQuality.Low, BirthdayAnimationQuality.Standard.lower())
        assertEquals(BirthdayAnimationQuality.Low, BirthdayAnimationQuality.Low.lower())
        assertTrue(BirthdayAnimationQuality.Refined.detail.nightStarCount > BirthdayAnimationQuality.Standard.detail.nightStarCount)
        assertTrue(BirthdayAnimationQuality.Refined.detail.starGlowStride > 0)
        assertTrue(BirthdayAnimationQuality.Refined.detail.meteorDebrisCount > 0)
        assertTrue(BirthdayAnimationQuality.Refined.detail.pondShimmerCount > 0)
        assertTrue(BirthdayAnimationQuality.Refined.detail.cakeShadowStride < Int.MAX_VALUE)
    }

    @Test
    fun developerPasscodeIgnoresOuterWhitespaceAndAsciiCase() {
        assertTrue(BirthdayAnimationDeveloperAccess.matchesPasscode("xiaoyu666"))
        assertTrue(BirthdayAnimationDeveloperAccess.matchesPasscode("  XiaoYu666\n"))
        assertFalse(BirthdayAnimationDeveloperAccess.matchesPasscode("xiaoyu665"))
        assertFalse(BirthdayAnimationDeveloperAccess.matchesPasscode(""))
    }
}
