package com.supernova.networkswitch.domain.usecase

import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.AutomationMode
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository

class CheckCompatibilityUseCase constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): CompatibilityState {
        val controlMethod = preferencesRepository.getControlMethod()
        return networkControlRepository.checkCompatibility(controlMethod)
    }
}

/**
 * Use case that explicitly (re-)triggers the Shizuku permission system dialog.
 * Used by the "Retry" button so a manual tap always re-prompts.
 */
class RequestShizukuPermissionUseCase constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    operator fun invoke() {
        networkControlRepository.requestShizukuPermission()
    }
}

class ToggleNetworkModeUseCase constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode> {
        return try {
            val toggleConfig = preferencesRepository.getToggleModeConfig()
            
            // Get the next mode to switch to (no current mode detection needed)
            val targetMode = toggleConfig.getNextMode()
            
            // Set the network mode
            val result = networkControlRepository.setNetworkMode(subId, targetMode)
            
            if (result.isSuccess) {
                // Update the toggle state for next time
                val newConfig = toggleConfig.toggle()
                preferencesRepository.setToggleModeConfig(newConfig)
            }
            
            result.map { targetMode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for getting current network mode
 */
class GetCurrentNetworkModeUseCase constructor(
    private val networkControlRepository: NetworkControlRepository
) {
    suspend operator fun invoke(subId: Int): Result<NetworkMode?> {
        return try {
            Result.success(networkControlRepository.getCurrentNetworkMode(subId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for updating control method preference
 */
class UpdateControlMethodUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(method: ControlMethod) {
        preferencesRepository.setControlMethod(method)
    }
}

/**
 * Use case for getting toggle mode configuration
 */
class GetToggleModeConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): ToggleModeConfig {
        return preferencesRepository.getToggleModeConfig()
    }
}

/**
 * Use case for updating toggle mode configuration
 */
class UpdateToggleModeConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: ToggleModeConfig) {
        preferencesRepository.setToggleModeConfig(config)
    }
}

class GetScreenStateConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): ScreenStateAutomationConfig = preferencesRepository.getScreenStateConfig()
}

class UpdateScreenStateConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: ScreenStateAutomationConfig) {
        preferencesRepository.setScreenStateConfig(config)
    }
}

class GetBatterySaverConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): BatterySaverAutomationConfig = preferencesRepository.getBatterySaverConfig()
}

class UpdateBatterySaverConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: BatterySaverAutomationConfig) {
        preferencesRepository.setBatterySaverConfig(config)
    }
}

class GetAppLaunchConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): AppLaunchAutomationConfig = preferencesRepository.getAppLaunchConfig()
}

class UpdateAppLaunchConfigUseCase constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(config: AppLaunchAutomationConfig) {
        preferencesRepository.setAppLaunchConfig(config)
    }
}

/**
 * Applies one of the two configured network modes (Mode A / Mode B) directly, and keeps the
 * manual toggle button (Network Mode card) in sync so the next manual tap offers the *other*
 * mode. Used by all three automation rules (screen state, battery saver, app launch) — none of
 * them go through [ToggleNetworkModeUseCase] since they need to set an *absolute* mode rather
 * than flip between whatever the current one happens to be.
 */
class ApplyAutomationModeUseCase constructor(
    private val networkControlRepository: NetworkControlRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(subId: Int, mode: AutomationMode): Result<NetworkMode> {
        return try {
            val toggleConfig = preferencesRepository.getToggleModeConfig()
            val targetMode = if (mode == AutomationMode.MODE_A) toggleConfig.modeA else toggleConfig.modeB

            val result = networkControlRepository.setNetworkMode(subId, targetMode)

            if (result.isSuccess) {
                // Keep the manual toggle in sync: after applying Mode A, the next manual tap
                // should offer Mode B, and vice versa.
                val newConfig = toggleConfig.copy(nextModeIsB = mode == AutomationMode.MODE_A)
                preferencesRepository.setToggleModeConfig(newConfig)
            }

            result.map { targetMode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
