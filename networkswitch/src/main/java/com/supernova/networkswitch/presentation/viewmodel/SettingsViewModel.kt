package com.supernova.networkswitch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.MasterSwitchStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

/**
 * Settings screen ViewModel using clean architecture
 */
class SettingsViewModel constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkControlRepository: NetworkControlRepository
) : ViewModel() {
    
    val controlMethod: StateFlow<ControlMethod> = preferencesRepository.observeControlMethod()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = ControlMethod.SHIZUKU
        )
    
    // Compatibility status for each method
    var rootCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    var shizukuCompatibility by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set
    
    init {
        checkAllCompatibility()
    }
    
    fun updateControlMethod(method: ControlMethod) {
        viewModelScope.launch {
            preferencesRepository.setControlMethod(method)
        }
    }
    
    fun retryCompatibilityCheck() {
        networkControlRepository.requestShizukuPermission()
        checkAllCompatibility()
    }

    /** Re-checks compatibility without forcing a new permission prompt — used on
     *  activity resume so returning from the Shizuku permission dialog (or any other
     *  app) reflects the latest state. */
    fun refreshCompatibility() {
        checkAllCompatibility()
    }
    
    private fun checkAllCompatibility() {
        if (!MasterSwitchStore.isEnabled()) {
            rootCompatibility = CompatibilityState.Incompatible("4G/5G Switcher is turned off")
            shizukuCompatibility = CompatibilityState.Incompatible("4G/5G Switcher is turned off")
            return
        }
        viewModelScope.launch {
            rootCompatibility = CompatibilityState.Pending
            shizukuCompatibility = CompatibilityState.Pending
            
            // Check both methods in parallel
            val rootResult = async { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
            val shizukuResult = async { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
            
            rootCompatibility = rootResult.await()
            shizukuCompatibility = shizukuResult.await()
        }
    }
}
