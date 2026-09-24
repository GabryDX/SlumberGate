package com.heronikostudios.slumbergate.data.model

data class UserSettings(
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val homeWifiSsid: String = "",
    val isActive: Boolean = true,
    val whitelistedPackages: Set<String> = emptySet(),
    val streakDays: Int = 0,
    val lastBypassTimestamp: Long = 0L,
    val lastCompletedDate: String = "",
    val isOnboardingCompleted: Boolean = false
) {
    val formattedBedtime: String
        get() = String.format("%02d:%02d", bedtimeHour, bedtimeMinute)

    val formattedWakeTime: String
        get() = String.format("%02d:%02d", wakeHour, wakeMinute)
}
