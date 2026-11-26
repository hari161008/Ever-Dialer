package com.modiprincekr.thedialer.view.screen.settings

import androidx.compose.runtime.Composable
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun BlurEffectsElementsScreen(navigator: DestinationsNavigator) {
    LiquidGlassElementsScreen(navigator = navigator, initialTab = 1)
}
