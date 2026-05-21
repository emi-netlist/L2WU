package com.emi.l2wu.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
    ServiceTrackerRepository is used to flag of the Service is started in ScreenControlService.kt, then this is remembered
    with the DataStore API.
 */
object ServiceTrackerRepository {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

}