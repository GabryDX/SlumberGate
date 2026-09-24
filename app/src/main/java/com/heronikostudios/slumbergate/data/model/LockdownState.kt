package com.heronikostudios.slumbergate.data.model

sealed interface LockdownState {
    data object Inactive : LockdownState

    data class WindDown(
        val remainingSeconds: Long,
        val totalSeconds: Long = 300L
    ) : LockdownState

    data object LockdownActive : LockdownState

    data class EmergencyUnlocked(
        val remainingSeconds: Long,
        val totalSeconds: Long = 180L
    ) : LockdownState
}
