package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    ExpandingScreenContainer(onClose = { navigator.navigateUp() }) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Call recording is not available in this version of Ever Dialer.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
