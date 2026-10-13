package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GamepadProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val opacity: Float = 0.65f,
    val scale: Float = 1.0f,
    val hapticFeedback: Boolean = true,
    val mappings: List<ButtonMapping>
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        mappings.forEach { array.put(it.toJsonObject()) }
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("opacity", opacity.toDouble())
            put("scale", scale.toDouble())
            put("hapticFeedback", hapticFeedback)
            put("mappings", array)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GamepadProfile {
            val mappingsList = mutableListOf<ButtonMapping>()
            val array = json.optJSONArray("mappings")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        mappingsList.add(ButtonMapping.fromJsonObject(item))
                    }
                }
            }
            return GamepadProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Preset"),
                description = json.optString("description", ""),
                opacity = json.optDouble("opacity", 0.65).toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat(),
                hapticFeedback = json.optBoolean("hapticFeedback", true),
                mappings = if (mappingsList.isNotEmpty()) mappingsList else createDefaultMappings()
            )
        }

        fun createDefaultMappings(): List<ButtonMapping> {
            return listOf(
                // Left side: Analog Joystick
                ButtonMapping(
                    key = ControlKey.JOYSTICK,
                    overlayXPercent = 0.16f,
                    overlayYPercent = 0.70f,
                    targetXPercent = 0.18f,
                    targetYPercent = 0.70f,
                    radiusDp = 64f,
                    targetRadiusPercent = 0.14f
                ),
                // Left side: D-Pad
                ButtonMapping(
                    key = ControlKey.DPAD_UP,
                    overlayXPercent = 0.16f,
                    overlayYPercent = 0.36f,
                    targetXPercent = 0.16f,
                    targetYPercent = 0.36f,
                    radiusDp = 30f
                ),
                ButtonMapping(
                    key = ControlKey.DPAD_DOWN,
                    overlayXPercent = 0.16f,
                    overlayYPercent = 0.50f,
                    targetXPercent = 0.16f,
                    targetYPercent = 0.50f,
                    radiusDp = 30f
                ),
                ButtonMapping(
                    key = ControlKey.DPAD_LEFT,
                    overlayXPercent = 0.08f,
                    overlayYPercent = 0.43f,
                    targetXPercent = 0.08f,
                    targetYPercent = 0.43f,
                    radiusDp = 30f
                ),
                ButtonMapping(
                    key = ControlKey.DPAD_RIGHT,
                    overlayXPercent = 0.24f,
                    overlayYPercent = 0.43f,
                    targetXPercent = 0.24f,
                    targetYPercent = 0.43f,
                    radiusDp = 30f
                ),

                // Right side: Action Quad (PlayStation glyphs)
                ButtonMapping(
                    key = ControlKey.ACTION_TRIANGLE,
                    overlayXPercent = 0.84f,
                    overlayYPercent = 0.55f,
                    targetXPercent = 0.84f,
                    targetYPercent = 0.55f,
                    radiusDp = 32f
                ),
                ButtonMapping(
                    key = ControlKey.ACTION_CIRCLE,
                    overlayXPercent = 0.92f,
                    overlayYPercent = 0.65f,
                    targetXPercent = 0.92f,
                    targetYPercent = 0.65f,
                    radiusDp = 32f
                ),
                ButtonMapping(
                    key = ControlKey.ACTION_CROSS,
                    overlayXPercent = 0.84f,
                    overlayYPercent = 0.75f,
                    targetXPercent = 0.84f,
                    targetYPercent = 0.75f,
                    radiusDp = 32f
                ),
                ButtonMapping(
                    key = ControlKey.ACTION_SQUARE,
                    overlayXPercent = 0.76f,
                    overlayYPercent = 0.65f,
                    targetXPercent = 0.76f,
                    targetYPercent = 0.65f,
                    radiusDp = 32f
                ),

                // Shoulder Bumpers (Top edges)
                ButtonMapping(
                    key = ControlKey.BUMPER_L1,
                    overlayXPercent = 0.15f,
                    overlayYPercent = 0.15f,
                    targetXPercent = 0.15f,
                    targetYPercent = 0.15f,
                    radiusDp = 38f
                ),
                ButtonMapping(
                    key = ControlKey.BUMPER_R1,
                    overlayXPercent = 0.85f,
                    overlayYPercent = 0.15f,
                    targetXPercent = 0.85f,
                    targetYPercent = 0.15f,
                    radiusDp = 38f
                ),

                // System buttons (Bottom center)
                ButtonMapping(
                    key = ControlKey.SELECT,
                    overlayXPercent = 0.42f,
                    overlayYPercent = 0.88f,
                    targetXPercent = 0.42f,
                    targetYPercent = 0.88f,
                    radiusDp = 26f
                ),
                ButtonMapping(
                    key = ControlKey.START,
                    overlayXPercent = 0.58f,
                    overlayYPercent = 0.88f,
                    targetXPercent = 0.58f,
                    targetYPercent = 0.88f,
                    radiusDp = 26f
                )
            )
        }

        fun createDefaultProfile(): GamepadProfile {
            return GamepadProfile(
                id = "default_preset",
                name = "Classic PSP Layout",
                description = "Balanced console layout with Joystick, D-Pad, and Action Glyphs.",
                opacity = 0.65f,
                scale = 1.0f,
                hapticFeedback = true,
                mappings = createDefaultMappings()
            )
        }

        fun createRpgProfile(): GamepadProfile {
            val mappings = createDefaultMappings().map { mapping ->
                when (mapping.key) {
                    ControlKey.ACTION_CROSS -> mapping.copy(targetXPercent = 0.88f, targetYPercent = 0.82f) // Attack
                    ControlKey.ACTION_CIRCLE -> mapping.copy(targetXPercent = 0.94f, targetYPercent = 0.70f) // Dash
                    ControlKey.ACTION_TRIANGLE -> mapping.copy(targetXPercent = 0.84f, targetYPercent = 0.60f) // Skill
                    ControlKey.ACTION_SQUARE -> mapping.copy(targetXPercent = 0.76f, targetYPercent = 0.72f) // Burst
                    else -> mapping
                }
            }
            return GamepadProfile(
                id = "action_rpg_preset",
                name = "Action RPG (Genshin / Honkai)",
                description = "Optimized for mobile 3D action games with right-hand attack clusters.",
                opacity = 0.60f,
                scale = 1.05f,
                hapticFeedback = true,
                mappings = mappings
            )
        }

        fun createFpsProfile(): GamepadProfile {
            val mappings = createDefaultMappings().map { mapping ->
                when (mapping.key) {
                    ControlKey.BUMPER_R1 -> mapping.copy(targetXPercent = 0.90f, targetYPercent = 0.45f) // Fire
                    ControlKey.BUMPER_L1 -> mapping.copy(targetXPercent = 0.12f, targetYPercent = 0.45f) // ADS
                    ControlKey.ACTION_CROSS -> mapping.copy(targetXPercent = 0.92f, targetYPercent = 0.82f) // Jump
                    ControlKey.ACTION_CIRCLE -> mapping.copy(targetXPercent = 0.84f, targetYPercent = 0.88f) // Crouch
                    else -> mapping
                }
            }
            return GamepadProfile(
                id = "fps_shooter_preset",
                name = "Shooter & Battle Royale",
                description = "Triggers configured for Fire & ADS, action buttons for Jump & Crouch.",
                opacity = 0.55f,
                scale = 1.1f,
                hapticFeedback = true,
                mappings = mappings
            )
        }
    }
}
