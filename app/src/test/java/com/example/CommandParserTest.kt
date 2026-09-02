package com.example

import com.example.commands.CommandIntent
import com.example.commands.CommandParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {

    private val parser = CommandParser()

    @Test
    fun testFlashlightCommands() {
        val cmdOn = parser.parse("Torch on karo")
        assertEquals(CommandIntent.TOGGLE_FLASHLIGHT, cmdOn.intent)
        assertEquals(true, cmdOn.enable)

        val cmdOff = parser.parse("Batti bujhao")
        assertEquals(CommandIntent.TOGGLE_FLASHLIGHT, cmdOff.intent)
        assertEquals(false, cmdOff.enable)
    }

    @Test
    fun testBatteryCommands() {
        val cmd = parser.parse("Battery kitni hai")
        assertEquals(CommandIntent.CHECK_BATTERY, cmd.intent)
    }

    @Test
    fun testVolumeCommands() {
        val cmdUp = parser.parse("Volume badhao")
        assertEquals(CommandIntent.VOLUME_CONTROL, cmdUp.intent)
        assertEquals(1, cmdUp.volumeLevel)

        val cmdDown = parser.parse("Aawaz kam karo")
        assertEquals(CommandIntent.VOLUME_CONTROL, cmdDown.intent)
        assertEquals(-1, cmdDown.volumeLevel)
    }

    @Test
    fun testSmartRoutines() {
        val morning = parser.parse("Good morning routine")
        assertEquals(CommandIntent.SMART_ROUTINE, morning.intent)
        assertEquals("morning", morning.routineType)

        val night = parser.parse("Good night lori")
        assertEquals(CommandIntent.SMART_ROUTINE, night.intent)
        assertEquals("night", night.routineType)
    }

    @Test
    fun testDiagnostics() {
        val cmd = parser.parse("Run system diagnostics")
        assertEquals(CommandIntent.DIAGNOSTICS, cmd.intent)
    }

    @Test
    fun testSosCommands() {
        val cmd = parser.parse("SOS strobe chalao")
        assertEquals(CommandIntent.TOGGLE_SOS_STROBE, cmd.intent)
    }

    @Test
    fun testWakeWordDetection() {
        val wakeKeywords = listOf("lori", "hey lori", "hi lori", "लोरी", "हे लोरी")
        val test1 = "Hey Lori torch on karo"
        val test2 = "Lori suno"
        val test3 = "Aaj ka mausam kaisa hai"

        val lower1 = test1.lowercase()
        val detected1 = wakeKeywords.any { kw -> lower1.contains(kw) }
        assertEquals(true, detected1)

        val lower2 = test2.lowercase()
        val detected2 = wakeKeywords.any { kw -> lower2.contains(kw) }
        assertEquals(true, detected2)

        val lower3 = test3.lowercase()
        val detected3 = wakeKeywords.any { kw -> lower3.contains(kw) }
        assertEquals(false, detected3)
    }

    @Test
    fun testWakeWordSensitivityAcousticCalculations() {
        val lowSensitivity = 0.30f
        val highSensitivity = 0.90f

        val lowSilenceWindow = (1600L + ((1.0f - lowSensitivity) * 2200L)).toLong()
        val highSilenceWindow = (1600L + ((1.0f - highSensitivity) * 2200L)).toLong()

        // Low sensitivity should allow a wider silence window (more patient)
        assertEquals(true, lowSilenceWindow > highSilenceWindow)
        assertEquals(3140L, lowSilenceWindow)
        assertEquals(1820L, highSilenceWindow)

        // Test RMS scaling
        val lowRmsScale = 0.6f + (lowSensitivity * 0.7f)
        val highRmsScale = 0.6f + (highSensitivity * 0.7f)
        assertEquals(true, highRmsScale > lowRmsScale)
    }
}
