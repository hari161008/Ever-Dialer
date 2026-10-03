package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.AppLanguageManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun LanguageSettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val settingsVer by prefs.settingsChanged.collectAsState()

    val currentLangCode = remember(settingsVer) {
        prefs.getAppLanguage()
    }

    val languages = remember {
        AppLanguageManager.SUPPORTED_LANGUAGES
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = com.coolappstore.everdialer.by.svhp.controller.util.tr("Languages"),
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Language Options ─────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(com.coolappstore.everdialer.by.svhp.controller.util.tr("Choose App Language"))
                    RivoExpressiveCard {
                        languages.forEachIndexed { index, lang ->
                            val isSelected = currentLangCode.equals(lang.code, ignoreCase = true)
                            val itemHeadline = if (lang.code == PreferenceManager.LANGUAGE_SYSTEM || lang.code == PreferenceManager.LANGUAGE_ENGLISH) {
                                lang.nativeTitle
                            } else {
                                "${lang.nativeTitle} (${lang.title})"
                            }
                            val itemSupporting = if (lang.code == PreferenceManager.LANGUAGE_SYSTEM) {
                                com.coolappstore.everdialer.by.svhp.controller.util.tr("Follow System")
                            } else {
                                lang.subtitle
                            }

                            RivoListItem(
                                headline = itemHeadline,
                                supporting = itemSupporting,
                                leadingIcon = Icons.Default.Translate,
                                iconContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                trailingIcon = if (isSelected) Icons.Default.Check else null,
                                trailingIconTint = MaterialTheme.colorScheme.primary,
                                trailingIconContainerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else null,
                                onClick = {
                                    if (!isSelected) {
                                        prefs.setAppLanguage(lang.code)
                                        AppLanguageManager.applyLocale(context, lang.code, recreateActivity = false)
                                    }
                                }
                            )

                            if (index < languages.lastIndex) {
                                CardDivider()
                            }
                        }
                    }
                }
            }

            // ── Information Note ─────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 120L) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = com.coolappstore.everdialer.by.svhp.controller.util.tr("Right-to-Left (RTL)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = com.coolappstore.everdialer.by.svhp.controller.util.tr("Selecting Arabic activates Right-to-Left layout throughout the app, mirroring navigation and swipes while preserving standard dialpad keys."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

