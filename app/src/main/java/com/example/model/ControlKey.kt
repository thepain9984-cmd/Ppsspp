package com.example.model

enum class ControlKey(val defaultLabel: String, val category: String) {
    // D-Pad
    DPAD_UP("▲", "D-Pad"),
    DPAD_DOWN("▼", "D-Pad"),
    DPAD_LEFT("◀", "D-Pad"),
    DPAD_RIGHT("▶", "D-Pad"),

    // Analog Joystick (Dynamic 360-degree direction)
    JOYSTICK("L-Stick", "Joystick"),

    // Action buttons (PSP / PlayStation glyphs)
    ACTION_TRIANGLE("△", "Action"),
    ACTION_CIRCLE("◯", "Action"),
    ACTION_CROSS("✕", "Action"),
    ACTION_SQUARE("◻", "Action"),

    // Shoulder bumpers
    BUMPER_L1("L1", "Triggers"),
    BUMPER_R1("R1", "Triggers"),
    TRIGGER_L2("L2", "Triggers"),
    TRIGGER_R2("R2", "Triggers"),

    // System keys
    SELECT("SELECT", "System"),
    START("START", "System")
}
