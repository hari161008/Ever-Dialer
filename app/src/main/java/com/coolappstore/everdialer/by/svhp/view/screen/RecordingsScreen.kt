package com.coolappstore.everdialer.by.svhp.view.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.DisclaimerScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.HomeScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PermissionsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PlaybackScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppNavigationViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * The "Recordings" tab shown in the main bottom navigation.
 *
 * This intentionally reuses the exact same [HomeScreen] and [PlaybackScreen] composables
 * that power the recording logs list inside Settings → Call Recording (the bundled
 * Ever Call Recorder module), so the list of call recordings looks and behaves identically
 * whether it's opened from here or from Settings.
 */
@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun RecordingsScreen(navController: NavController, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appNavViewModel: AppNavigationViewModel = viewModel()
    val onboardingStatus by appNavViewModel.onboardingStatus.collectAsState()

    var selectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
    var highlightQuery by remember { mutableStateOf("") }
    var isRecordingSelectionMode by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedRecording != null) {
        selectedRecording = null
        highlightQuery = ""
    }

    // Re-check disclaimer/permission state whenever this tab (re)appears or the app resumes,
    // so granting a permission (or accepting the disclaimer) refreshes the gate immediately.
    LaunchedEffect(Unit) { appNavViewModel.refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appNavViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Hide the bottom navigation bar (pill/standard) while the disclaimer or permissions
    // onboarding content is showing, since its "Continue"/"Grant Access" button sits at
    // the bottom of the screen where the nav bar would otherwise cover it.
    val isShowingOnboarding = !onboardingStatus.disclaimerAccepted || !onboardingStatus.isComplete()
    DisposableEffect(isShowingOnboarding) {
        NavBarVisibilityState.hideForOnboarding = isShowingOnboarding
        onDispose { NavBarVisibilityState.hideForOnboarding = false }
    }

    // Smoothly slide the main bottom navigation pill out of the way while the recordings
    // list's own selection pill (Favourite / Recover / Assign contact / Recordings / Share)
    // is showing, so the two pills never overlap.
    DisposableEffect(isRecordingSelectionMode) {
        NavBarVisibilityState.hideForSelectionMode = isRecordingSelectionMode
        onDispose { NavBarVisibilityState.hideForSelectionMode = false }
    }

    val appVersion = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            !onboardingStatus.disclaimerAccepted -> {
                DisclaimerScreen(
                    onContinue = {
                        AppPreferences(context).setDisclaimerAccepted(true)
                        appNavViewModel.refresh()
                    }
                )
            }
            !onboardingStatus.isComplete() -> {
                PermissionsScreen(
                    status = onboardingStatus,
                    onPermissionGranted = { appNavViewModel.refresh() }
                )
            }
            else -> {
                AnimatedContent(
                    targetState = selectedRecording,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "RecordingsTabContent"
                ) { recording ->
                    if (recording != null) {
                        PlaybackScreen(
                            recording = recording,
                            onBack = { selectedRecording = null; highlightQuery = "" },
                            highlightQuery = highlightQuery
                        )
                    } else {
                        HomeScreen(
                            appVersion = appVersion,
                            onSettingsClick = {
                                // Opens the bundled Ever Call Recorder app directly on its
                                // Call Recording settings screen (storage location, filename
                                // format, auto-delete rules, etc.), skipping the recordings list.
                                val launch = Intent(context, com.coolappstore.evercallrecorder.by.svhp.MainActivity::class.java)
                                launch.putExtra(com.coolappstore.evercallrecorder.by.svhp.MainActivity.EXTRA_OPEN_SETTINGS, true)
                                context.startActivity(launch)
                            },
                            onRecordingClick = { recordingItem, query ->
                                selectedRecording = recordingItem
                                highlightQuery = query
                            },
                            onSelectionModeChanged = { isRecordingSelectionMode = it }
                        )
                    }
                }
            }
        }
    }
}
