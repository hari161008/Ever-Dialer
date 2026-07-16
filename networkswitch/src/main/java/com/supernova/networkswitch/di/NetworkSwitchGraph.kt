package com.supernova.networkswitch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.supernova.networkswitch.data.repository.NetworkControlRepositoryImpl
import com.supernova.networkswitch.data.repository.PreferencesRepositoryImpl
import com.supernova.networkswitch.data.source.PreferencesDataSource
import com.supernova.networkswitch.data.source.RootNetworkControlDataSource
import com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.RequestShizukuPermissionUseCase
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.usecase.UpdateToggleModeConfigUseCase

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "network_switch_preferences")

/**
 * Lightweight, hand-rolled dependency graph for the bundled Network Switch feature.
 *
 * This replaces the original Hilt-based [dagger.hilt.android.HiltAndroidApp] / `@Module` wiring:
 * the Hilt Gradle plugin fails to apply under this project's AGP version ("Android BaseExtension
 * not found"), so the exact same singletons that Hilt would have provided are constructed here
 * instead. Behavior and the object graph shape are unchanged from the original DataModule.
 */
object NetworkSwitchGraph {

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun requireContext(): Context =
        appContext ?: throw IllegalStateException("NetworkSwitchGraph.init(context) must be called before use")

    val dataStore: DataStore<Preferences> by lazy { requireContext().dataStore }

    val preferencesDataSource: PreferencesDataSource by lazy { PreferencesDataSource(dataStore) }

    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepositoryImpl(preferencesDataSource) }

    val rootNetworkControlDataSource: RootNetworkControlDataSource by lazy { RootNetworkControlDataSource(requireContext()) }

    val shizukuNetworkControlDataSource: ShizukuNetworkControlDataSource by lazy { ShizukuNetworkControlDataSource(requireContext()) }

    val networkControlRepository: NetworkControlRepository by lazy {
        NetworkControlRepositoryImpl(rootNetworkControlDataSource, shizukuNetworkControlDataSource, preferencesRepository)
    }

    val checkCompatibilityUseCase: CheckCompatibilityUseCase by lazy {
        CheckCompatibilityUseCase(networkControlRepository, preferencesRepository)
    }

    val requestShizukuPermissionUseCase: RequestShizukuPermissionUseCase by lazy {
        RequestShizukuPermissionUseCase(networkControlRepository)
    }

    val toggleNetworkModeUseCase: ToggleNetworkModeUseCase by lazy {
        ToggleNetworkModeUseCase(networkControlRepository, preferencesRepository)
    }

    val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase by lazy {
        GetCurrentNetworkModeUseCase(networkControlRepository)
    }

    val updateControlMethodUseCase: UpdateControlMethodUseCase by lazy {
        UpdateControlMethodUseCase(preferencesRepository)
    }

    val getToggleModeConfigUseCase: GetToggleModeConfigUseCase by lazy {
        GetToggleModeConfigUseCase(preferencesRepository)
    }

    val updateToggleModeConfigUseCase: UpdateToggleModeConfigUseCase by lazy {
        UpdateToggleModeConfigUseCase(preferencesRepository)
    }
}
