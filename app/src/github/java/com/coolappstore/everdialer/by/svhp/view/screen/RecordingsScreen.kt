package com.coolappstore.everdialer.by.svhp.view.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import com.coolappstore.everdialer.by.svhp.view.components.SettingsIconButton
import com.coolappstore.everdialer.by.svhp.view.components.MultiCallContactSelectionDialog
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.AppLockScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.DisclaimerScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.HomeScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PermissionsScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PlaybackScreen
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppLockViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppNavigationViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun RecordingsScreen(
    openedFromSettings: Boolean = false,
    openedRecordingUri: String? = null,
    navController: NavController,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appNavViewModel: AppNavigationViewModel = viewModel()
    val onboardingStatus by appNavViewModel.onboardingStatus.collectAsState()
    val appLockViewModel: AppLockViewModel = viewModel()
    val isAppLockUnlocked by appLockViewModel.isUnlocked.collectAsState()
    val preferences = remember { AppPreferences(context) }
    val recordingsVM: HomeViewModel = viewModel()
    val recordingsForLookup by recordingsVM.allRecordings.collectAsState()

    var selectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
    var highlightQuery by remember { mutableStateOf("") }
    var isRecordingSelectionMode by remember { mutableStateOf(false) }
    var directEntryUriToOpen by remember { mutableStateOf(openedRecordingUri) }
    var multiCallItemToSelect by remember { mutableStateOf<RecordingItem?>(null) }

    val onOpenContactInfo: (RecordingItem) -> Unit = { item ->
        if (item.isMultiCall && !item.secondPhoneNumber.isNullOrBlank()) {
            multiCallItemToSelect = item
        } else {
            navigator.navigate(
                ContactDetailsScreenDestination(
                    contactId = item.contactId ?: "null",
                    phoneNumber = item.phoneNumber
                )
            )
        }
    }

    LaunchedEffect(directEntryUriToOpen, recordingsForLookup) {
        val uri = directEntryUriToOpen
        if (!uri.isNullOrEmpty() && selectedRecording == null) {
            recordingsForLookup.firstOrNull {
                it.uri.toString() == uri || android.net.Uri.decode(it.uri.toString()) == android.net.Uri.decode(uri)
            }?.let {
                selectedRecording = it
                directEntryUriToOpen = null
            }
        }
    }

    val isAppLocked = onboardingStatus.disclaimerAccepted && onboardingStatus.isComplete() &&
        preferences.isAppLockEnabled() && !isAppLockUnlocked

    BackHandler(enabled = selectedRecording != null && !isAppLocked) {
        selectedRecording = null
        highlightQuery = ""
    }

    LaunchedEffect(Unit) { appNavViewModel.refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> appNavViewModel.refresh()
                Lifecycle.Event.ON_STOP -> {
                    val isConfigChange = (context as? Activity)?.isChangingConfigurations == true
                    if (!isConfigChange) appLockViewModel.lock()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isShowingOnboarding = !onboardingStatus.disclaimerAccepted || !onboardingStatus.isComplete() || isAppLocked
    DisposableEffect(isShowingOnboarding) {
        NavBarVisibilityState.hideForOnboarding = isShowingOnboarding
        onDispose { NavBarVisibilityState.hideForOnboarding = false }
    }

    DisposableEffect(isRecordingSelectionMode) {
        NavBarVisibilityState.hideForSelectionMode = isRecordingSelectionMode
        onDispose { NavBarVisibilityState.hideForSelectionMode = false }
    }

    DisposableEffect(openedFromSettings) {
        NavBarVisibilityState.hideForSettingsEntry = openedFromSettings
        onDispose {
            NavBarVisibilityState.hideForSettingsEntry = false
            if (openedFromSettings) {
                com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.notifyClosing()
                com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.triggerSettingsCloseBounce()
            }
        }
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
                    onPermissionGranted = { appNavViewModel.refresh() },
                    onSkip = { appNavViewModel.skipOnboarding() }
                )
            }
            isAppLocked -> {
                AppLockScreen(
                    method = preferences.getAppLockMethod(),
                    onVerifySecret = { secret -> preferences.verifyAppLockSecret(secret) },
                    onUnlocked = { appLockViewModel.unlock() }
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        appVersion = appVersion,
                        onBackClick = if (openedFromSettings) {
                            {
                                com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.notifyClosing()
                                com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.triggerSettingsCloseBounce()
                                navigator.navigateUp()
                            }
                        } else null,
                        onSettingsClick = {
                            com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.recordNormalized(0.92f, 0.06f)
                            navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CallRecordingSettingsScreenDestination())
                        },
                        onRecordingClick = { recordingItem, query ->
                            selectedRecording = recordingItem
                            highlightQuery = query
                        },
                        onSelectionModeChanged = { isRecordingSelectionMode = it },
                        onGlobalSearchClick = {
                            navigator.navigate(com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination())
                        },
                        onEverDialerSettingsClick = if (openedFromSettings) { {} } else {
                            { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination()) }
                        },
                        everDialerSettingsButton = if (openedFromSettings) null else {
                            {
                                SettingsIconButton(
                                    navigator = navigator,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        },
                        onOpenContactInfo = onOpenContactInfo
                    )

                    var lastSelectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
                    if (selectedRecording != null) {
                        lastSelectedRecording = selectedRecording
                    }

                    AnimatedVisibility(
                        visible = selectedRecording != null,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(160))
                    ) {
                        lastSelectedRecording?.let { recording ->
                            PlaybackScreen(
                                recording = recording,
                                onBack = {
                                    selectedRecording = null
                                    highlightQuery = ""
                                },
                                highlightQuery = highlightQuery,
                                onOpenContactInfo = onOpenContactInfo,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (multiCallItemToSelect != null) {
                        val multiItem = multiCallItemToSelect!!
                        MultiCallContactSelectionDialog(
                            name1 = multiItem.contactName,
                            number1 = multiItem.phoneNumber,
                            contactId1 = multiItem.contactId,
                            photoUri1 = multiItem.photoUri,
                            name2 = multiItem.secondContactName,
                            number2 = multiItem.secondPhoneNumber ?: "",
                            contactId2 = multiItem.secondContactId,
                            photoUri2 = multiItem.secondPhotoUri,
                            isMergedCall = multiItem.isMergedCall,
                            onSelectContact = { selectedId, selectedNum ->
                                multiCallItemToSelect = null
                                navigator.navigate(
                                    ContactDetailsScreenDestination(
                                        contactId = selectedId ?: "null",
                                        phoneNumber = selectedNum
                                    )
                                )
                            },
                            onDismiss = { multiCallItemToSelect = null }
                        )
                    }
                }
            }
        }
    }
}
