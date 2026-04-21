package com.grinch.rivo4.view.screen

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.grinch.rivo4.controller.ContactsViewModel
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun ContactScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    var visible by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )

    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(navController, navigator) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier.scale(fabScale)
            ) {
                Icon(Icons.Default.PersonAdd, "Add Contact")
            }
        },
        bottomBar = { BottomBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ContactContent(
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState
            )
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    }
}

@Composable
fun ContactContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        if (isGranted) {
            val contactsVM: ContactsViewModel = koinActivityViewModel()
            val contacts = contactsVM.allContacts.collectAsState().value

            if (contacts.isEmpty()) {
                RivoLoadingIndicatorView()
            } else {
                AZListScroll(contacts, navigator, listState = listState)
            }
        } else {
            PermissionDeniedView(
                icon = Icons.Default.Person,
                title = "Contacts",
                description = "Ever Dialer needs access to your contacts to show your contact list and identify incoming calls.",
                onGrantClick = onRequestPermission
            )
        }
    }
}
