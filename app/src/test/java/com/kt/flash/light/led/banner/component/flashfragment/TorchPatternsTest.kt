package com.kt.flash.light.led.banner.component.flashfragment

import org.junit.Assert.*
import org.junit.Test

class TorchPatternsTest {
    @Test fun sosSendsThreeDotsThreeDashesThreeDotsWithLetterAndRepeatGaps() {
        val steps = TorchPatterns.steps(TorchPatterns.SOS)
        assertEquals(listOf(200L, 200L, 200L, 600L, 600L, 600L, 200L, 200L, 200L),
            steps.filter { it.enabled }.map { it.durationMs })
        assertEquals(listOf(200L, 200L, 600L, 200L, 200L, 600L, 200L, 200L, 1400L),
            steps.filterNot { it.enabled }.map { it.durationMs })
        assertTrue(steps.first().enabled)
        assertFalse(steps.last().enabled)
        assertTrue(steps.zipWithNext().all { (first, second) -> first.enabled != second.enabled })
    }

    @Test fun djRepeatsAnOnOffPairAndSteadyFlashHasNoBlinkTimer() {
        val dj = TorchPatterns.steps(TorchPatterns.DJ)
        assertEquals(listOf(true, false), dj.map { it.enabled })
        assertTrue(dj.all { it.durationMs == 150L })
        assertTrue(TorchPatterns.steps(TorchPatterns.FLASH).isEmpty())
    }
}
