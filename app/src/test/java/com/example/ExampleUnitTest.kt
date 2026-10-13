package com.example

import com.example.model.ControlKey
import com.example.model.GamepadProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun defaultProfile_containsAllEssentialControls() {
        val profile = GamepadProfile.createDefaultProfile()
        assertNotNull(profile)
        assertEquals("Classic PSP Layout", profile.name)

        val keys = profile.mappings.map { it.key }.toSet()
        assertTrue(keys.contains(ControlKey.JOYSTICK))
        assertTrue(keys.contains(ControlKey.DPAD_UP))
        assertTrue(keys.contains(ControlKey.DPAD_DOWN))
        assertTrue(keys.contains(ControlKey.DPAD_LEFT))
        assertTrue(keys.contains(ControlKey.DPAD_RIGHT))
        assertTrue(keys.contains(ControlKey.ACTION_CROSS))
        assertTrue(keys.contains(ControlKey.ACTION_CIRCLE))
        assertTrue(keys.contains(ControlKey.ACTION_TRIANGLE))
        assertTrue(keys.contains(ControlKey.ACTION_SQUARE))
        assertTrue(keys.contains(ControlKey.BUMPER_L1))
        assertTrue(keys.contains(ControlKey.BUMPER_R1))
        assertTrue(keys.contains(ControlKey.SELECT))
        assertTrue(keys.contains(ControlKey.START))
    }

    @Test
    fun profile_jsonSerialization_roundTripPreservesValues() {
        val original = GamepadProfile.createRpgProfile()
        val json = original.toJsonObject()
        val restored = GamepadProfile.fromJsonObject(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.opacity, restored.opacity, 0.01f)
        assertEquals(original.scale, restored.scale, 0.01f)
        assertEquals(original.mappings.size, restored.mappings.size)

        val origCross = original.mappings.first { it.key == ControlKey.ACTION_CROSS }
        val restCross = restored.mappings.first { it.key == ControlKey.ACTION_CROSS }
        assertEquals(origCross.targetXPercent, restCross.targetXPercent, 0.01f)
        assertEquals(origCross.targetYPercent, restCross.targetYPercent, 0.01f)
    }
}
