package com.example.model

import org.json.JSONObject

data class ButtonMapping(
    val key: ControlKey,
    val customLabel: String = key.defaultLabel,
    val overlayXPercent: Float, // 0f..1f relative to overlay canvas
    val overlayYPercent: Float, // 0f..1f relative to overlay canvas
    val targetXPercent: Float,  // 0f..1f relative to physical screen
    val targetYPercent: Float,  // 0f..1f relative to physical screen
    val radiusDp: Float = 36f,
    val targetRadiusPercent: Float = 0.12f, // For joystick max touch drag radius
    val isEnabled: Boolean = true,
    val isHoldSupported: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("key", key.name)
            put("customLabel", customLabel)
            put("overlayXPercent", overlayXPercent.toDouble())
            put("overlayYPercent", overlayYPercent.toDouble())
            put("targetXPercent", targetXPercent.toDouble())
            put("targetYPercent", targetYPercent.toDouble())
            put("radiusDp", radiusDp.toDouble())
            put("targetRadiusPercent", targetRadiusPercent.toDouble())
            put("isEnabled", isEnabled)
            put("isHoldSupported", isHoldSupported)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ButtonMapping {
            val key = try {
                ControlKey.valueOf(json.optString("key", ControlKey.ACTION_CROSS.name))
            } catch (e: Exception) {
                ControlKey.ACTION_CROSS
            }
            return ButtonMapping(
                key = key,
                customLabel = json.optString("customLabel", key.defaultLabel),
                overlayXPercent = json.optDouble("overlayXPercent", 0.5).toFloat(),
                overlayYPercent = json.optDouble("overlayYPercent", 0.5).toFloat(),
                targetXPercent = json.optDouble("targetXPercent", 0.5).toFloat(),
                targetYPercent = json.optDouble("targetYPercent", 0.5).toFloat(),
                radiusDp = json.optDouble("radiusDp", 36.0).toFloat(),
                targetRadiusPercent = json.optDouble("targetRadiusPercent", 0.12).toFloat(),
                isEnabled = json.optBoolean("isEnabled", true),
                isHoldSupported = json.optBoolean("isHoldSupported", true)
            )
        }
    }
}
