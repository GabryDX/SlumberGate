package com.heronikostudios.slumbergate.core.service

import com.heronikostudios.slumbergate.data.model.LockdownState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LockdownStateHolder {
    private val _lockdownState = MutableStateFlow<LockdownState>(LockdownState.Inactive)
    val lockdownState = _lockdownState.asStateFlow()

    @Volatile
    var isLockdownActive: Boolean = false
        private set

    @Volatile
    var isEmergencyUnlocked: Boolean = false
        private set

    @Volatile
    var isPhoneCallActive: Boolean = false

    @Volatile
    var whitelistedPackages: Set<String> = emptySet()

    fun updateState(state: LockdownState) {
        _lockdownState.value = state
        when (state) {
            is LockdownState.LockdownActive -> {
                isLockdownActive = true
                isEmergencyUnlocked = false
            }
            is LockdownState.EmergencyUnlocked -> {
                isLockdownActive = false
                isEmergencyUnlocked = true
            }
            is LockdownState.WindDown -> {
                isLockdownActive = false
                isEmergencyUnlocked = false
            }
            is LockdownState.Inactive -> {
                isLockdownActive = false
                isEmergencyUnlocked = false
            }
        }
    }
}
