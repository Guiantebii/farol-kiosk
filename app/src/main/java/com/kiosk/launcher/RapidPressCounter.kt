package com.kiosk.launcher

class RapidPressCounter(
    private val requiredPresses: Int = 7,
    private val windowMs: Long = 2000L,
    private val onTriggered: () -> Unit
) {
    private val timestamps = ArrayDeque<Long>()

    fun registerPress() {
        val now = System.currentTimeMillis()
        while (timestamps.isNotEmpty() && now - timestamps.first() > windowMs) {
            timestamps.removeFirst()
        }
        timestamps.addLast(now)
        if (timestamps.size >= requiredPresses) {
            timestamps.clear()
            onTriggered()
        }
    }

    fun reset() {
        timestamps.clear()
    }
}
