package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.NetworkControlDataSource
import com.supernova.networkswitch.data.source.RootNetworkControlDataSource
import com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.MasterSwitchStore

/**
 * Implementation of NetworkControlRepository that delegates to appropriate data source.
 *
 * This is the single gateway every caller (view models, the Quick Settings tile) goes through to
 * reach the Shizuku/root data sources, so it is also where the master kill switch is enforced.
 * When [MasterSwitchStore.isEnabled] is false, every method below returns immediately without
 * touching [rootDataSource] or [shizukuDataSource] at all — nothing runs, in the foreground or in
 * the background.
 */
class NetworkControlRepositoryImpl constructor(
    private val rootDataSource: RootNetworkControlDataSource,
    private val shizukuDataSource: ShizukuNetworkControlDataSource,
    private val preferencesRepository: PreferencesRepository
) : NetworkControlRepository {
    
    override suspend fun checkCompatibility(method: ControlMethod): CompatibilityState {
        if (!MasterSwitchStore.isEnabled()) {
            return CompatibilityState.Incompatible("4G/5G Switcher is turned off")
        }
        val dataSource = getDataSource(method)
        val subId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
        return dataSource.checkCompatibility(subId)
    }

    override suspend fun getCurrentNetworkMode(subId: Int): NetworkMode? {
        if (!MasterSwitchStore.isEnabled()) return null
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            dataSource.getCurrentNetworkMode(subId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit> {
        if (!MasterSwitchStore.isEnabled()) {
            return Result.failure(IllegalStateException("4G/5G Switcher is turned off"))
        }
        return try {
            val method = preferencesRepository.getControlMethod()
            val dataSource = getDataSource(method)
            dataSource.setNetworkMode(subId, mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset connections for both data sources - useful when switching control methods
     */
    override suspend fun resetConnections() {
        if (!MasterSwitchStore.isEnabled()) return
        rootDataSource.resetConnection()
        shizukuDataSource.resetConnection()
    }

    override fun requestShizukuPermission() {
        if (!MasterSwitchStore.isEnabled()) return
        shizukuDataSource.requestPermission()
    }

    private fun getDataSource(method: ControlMethod): NetworkControlDataSource {
        return when (method) {
            ControlMethod.ROOT -> rootDataSource
            ControlMethod.SHIZUKU -> shizukuDataSource
        }
    }
}
