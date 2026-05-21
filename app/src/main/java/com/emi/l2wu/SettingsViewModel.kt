package com.emi.l2wu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emi.l2wu.datastore.SettingsManager
import com.emi.l2wu.repository.ServiceTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
    The ViewModel acts as a bridge. It reads data stream from SettingsManager and converts it
    into a Compose-friendly StateFlow. It also provides a method to trigger updates using a coroutine.
 */

// Using AndroidViewModel to easily get application context for DataStore
class SettingsViewModel(
    application: Application
    ) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    init {
        // If the Service was killed the variable is false, and the UI buttons are re-enabled
        setServiceStarted(ServiceTrackerRepository.isServiceRunning.value)
    }

    // Convert Flow into StateFlow so Compose can seamlessly observe it
    val isServiceStarted: StateFlow<Boolean> = settingsManager.isServiceStarted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Temporary fallback value before disk read completes
        )

    fun setServiceStarted(isEnabled: Boolean): Unit {
        viewModelScope.launch {
            settingsManager.setIsServiceStarted(isEnabled)
        }
    }
}