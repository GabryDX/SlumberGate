package com.heronikostudios.slumbergate.data.model

data class SessionStats(
    val streakDays: Int = 0,
    val lastCompletedDate: String = "",
    val totalLockdownsCompleted: Int = 0,
    val bypassCount: Int = 0
)
