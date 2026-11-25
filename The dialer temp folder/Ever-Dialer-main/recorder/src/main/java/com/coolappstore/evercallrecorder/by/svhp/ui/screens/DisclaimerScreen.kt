/*
 * Ever Call Recorder
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.evercallrecorder.by.svhp.AppUrls
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.ui.theme.ShizucallrecorderTheme

@Composable
fun DisclaimerScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    var hasAccepted by rememberSaveable { mutableStateOf(false) }
    var hasScrolledToBottom by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.canScrollForward) {
        if (!scrollState.canScrollForward) hasScrolledToBottom = true
    }

    val primary       = MaterialTheme.colorScheme.primary
    val primaryCont   = MaterialTheme.colorScheme.primaryContainer
    val secondaryCont = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryCont  = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimaryCont = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Banner ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to primaryCont,
                                0.5f to secondaryCont,
                                1.0f to tertiaryCont
                            )
                        )
                    )
            ) {
                // Decorative circles — clean, no blur
                Box(Modifier.size(130.dp).align(Alignment.TopEnd).offset(36.dp, (-36).dp).clip(CircleShape).background(onPrimaryCont.copy(alpha = 0.10f)))
                Box(Modifier.size(70.dp).align(Alignment.TopEnd).offset(10.dp, (-4).dp).clip(CircleShape).background(primary.copy(alpha = 0.15f)))
                Box(Modifier.size(110.dp).align(Alignment.BottomStart).offset((-32).dp, 32.dp).clip(CircleShape).background(onPrimaryCont.copy(alpha = 0.09f)))
                Box(Modifier.size(50.dp).align(Alignment.BottomStart).offset((-8).dp, 8.dp).clip(CircleShape).background(primary.copy(alpha = 0.13f)))
                Box(Modifier.size(18.dp).align(Alignment.CenterEnd).offset((-28).dp, (-20).dp).clip(CircleShape).background(onPrimaryCont.copy(alpha = 0.22f)))
                Box(Modifier.size(12.dp).offset(28.dp, 32.dp).clip(CircleShape).background(onPrimaryCont.copy(alpha = 0.18f)))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(onPrimaryCont.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Gavel, null, tint = onPrimaryCont, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.disclaimer_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimaryCont,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    val links = mapOf(stringResource(R.string.disclaimer_wiki_link_KEYWORD) to AppUrls.GITHUB_WIKI)
                    HyperlinkText(
                        fullText = stringResource(R.string.disclaimer_introduction),
                        links = links,
                        baseColor = onPrimaryCont.copy(alpha = 0.80f),
                        linkColor = onPrimaryCont
                    )
                }
            }

            // ── Disclaimer body ───────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp
            ) {
                SelectionContainer {
                    Text(
                        text = stringResource(R.string.disclaimer_body),
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            // ── Footer ────────────────────────────────────────────────────────
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Checkbox row
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasScrolledToBottom) primaryCont.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = hasAccepted,
                                    onValueChange = { if (hasScrolledToBottom) hasAccepted = it },
                                    role = Role.Checkbox,
                                    enabled = hasScrolledToBottom
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = hasAccepted,
                                onCheckedChange = null,
                                enabled = hasScrolledToBottom
                            )
                            Text(
                                text = stringResource(R.string.disclaimer_checkbox_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasScrolledToBottom) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = hasAccepted && hasScrolledToBottom,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            if (!hasScrolledToBottom) Icons.Outlined.ArrowDownward
                            else if (!hasAccepted) Icons.Outlined.CheckBoxOutlineBlank
                            else Icons.Outlined.Check,
                            null, Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (!hasScrolledToBottom) stringResource(R.string.disclaimer_must_read)
                                   else stringResource(R.string.general_continue),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HyperlinkText(
    fullText: String,
    links: Map<String, String>,
    modifier: Modifier = Modifier,
    baseColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    linkColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val uriHandler = LocalUriHandler.current
    val listener = LinkInteractionListener { link ->
        if (link is LinkAnnotation.Clickable) uriHandler.openUri(link.tag)
    }
    val annotatedText = buildAnnotatedString {
        withStyle(SpanStyle(color = baseColor)) { append(fullText) }
        links.forEach { (keyword, url) ->
            val startIndex = fullText.indexOf(keyword)
            if (startIndex != -1) {
                addLink(
                    clickable = LinkAnnotation.Clickable(
                        tag = url,
                        styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = linkColor, fontWeight = FontWeight.Bold)),
                        linkInteractionListener = listener
                    ),
                    start = startIndex,
                    end = startIndex + keyword.length
                )
            }
        }
    }
    Text(text = annotatedText, style = MaterialTheme.typography.bodySmall, modifier = modifier, lineHeight = 16.sp)
}

@Preview(showBackground = true)
@Composable
private fun DisclaimerScreenPreview() {
    ShizucallrecorderTheme { DisclaimerScreen(onContinue = {}) }
}
