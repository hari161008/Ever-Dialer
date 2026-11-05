package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.SettingsScreen as RecorderSettingsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.SettingsViewModel
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import com.coolappstore.everdialer.by.svhp.view.theme.ExpandingScreenContainer
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallRecordingSettingsScreen(
    navigator: DestinationsNavigator
) {
    DisposableEffect(Unit) {
        NavBarVisibilityState.hideForSettingsEntry = true
        onDispose {
            NavBarVisibilityState.hideForSettingsEntry = false
        }
    }

    ExpandingScreenContainer(onClose = { navigator.navigateUp() }) { closeWithAnimation ->
        val recorderSettingsViewModel: SettingsViewModel = viewModel()
        RecorderSettingsScreen(
            viewModel = recorderSettingsViewModel,
            onBack = { closeWithAnimation() },
            enableBackHandler = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}
