package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PfpShapeRegistry
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.RotatingPfpShapeContainer
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun PfpShapeChooserScreen(
    navigator: DestinationsNavigator,
    title: String = "PFP Shape",
    shapePrefKey: String,
    animatePrefKey: String
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var animateRotation by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(animatePrefKey, false))
    }
    val rawShapePref = remember(settingsVer) {
        prefs.getString(shapePrefKey, "circle") ?: "circle"
    }
    var selectedKeys by remember(rawShapePref) {
        mutableStateOf(PfpShapeRegistry.parseShapeKeys(rawShapePref).toSet())
    }
    var chooseMultipleShapes by remember(rawShapePref) {
        mutableStateOf(PfpShapeRegistry.parseShapeKeys(rawShapePref).size > 1)
    }

    fun saveShapes(keys: Set<String>) {
        selectedKeys = keys
        val stringValue = keys.joinToString(",")
        prefs.setString(shapePrefKey, stringValue)
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = title,
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Selected shape label banner
            item(span = { GridItemSpan(3) }) {
                RivoAnimatedSection(delayMs = 0L) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedKeys.size == 1) {
                                PfpShapeRegistry.getShapeItem(selectedKeys.first()).label
                            } else {
                                "${selectedKeys.size} shapes selected (randomizes per contact)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedKeys.size == 1) {
                                "Tap any shape below to apply it"
                            } else {
                                "Contacts will pick randomly among these selected shapes"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Options card: Animate Shape Rotation + Choose Multiple Shapes
            item(span = { GridItemSpan(3) }) {
                RivoAnimatedSection(delayMs = 15L) {
                    Column {
                        Text(
                            "Shape Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Animate Shape Rotation",
                                supporting = "Smooth 360° rotating mask while contact photo stays upright",
                                checked = animateRotation,
                                onCheckedChange = { isChecked ->
                                    animateRotation = isChecked
                                    prefs.setBoolean(animatePrefKey, isChecked)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            RivoSwitchListItem(
                                headline = "Choose Multiple Shapes",
                                supporting = "Select multiple designs (randomizes per contact)",
                                checked = chooseMultipleShapes,
                                onCheckedChange = { isChecked ->
                                    chooseMultipleShapes = isChecked
                                    if (!isChecked && selectedKeys.size > 1) {
                                        saveShapes(setOf(selectedKeys.first()))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section label for shapes grid
            item(span = { GridItemSpan(3) }) {
                RivoAnimatedSection(delayMs = 25L) {
                    Text(
                        "Available Shapes (${PfpShapeRegistry.allShapes.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // 32 Shapes
            items(PfpShapeRegistry.allShapes.size) { index ->
                val item = PfpShapeRegistry.allShapes[index]
                val isSelected = selectedKeys.contains(item.key)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (chooseMultipleShapes) {
                                val nextKeys = if (isSelected) {
                                    if (selectedKeys.size > 1) selectedKeys - item.key else selectedKeys
                                } else {
                                    selectedKeys + item.key
                                }
                                saveShapes(nextKeys)
                            } else {
                                saveShapes(setOf(item.key))
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 2.dp, top = 2.dp)
                                    .size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RotatingPfpShapeContainer(
                                shapeKey = item.key,
                                animateRotation = animateRotation,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Surface(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = item.label.firstOrNull()?.toString() ?: "?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
