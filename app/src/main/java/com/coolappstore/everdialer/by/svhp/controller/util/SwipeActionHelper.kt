package com.coolappstore.everdialer.by.svhp.controller.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ColorScheme
import com.coolappstore.evercallrecorder.by.svhp.ui.common.SwipeActionItem

object SwipeActionHelper {

    fun resolveActionItem(actionKey: String, colorScheme: ColorScheme): SwipeActionItem? {
        return when (actionKey) {
            "call", "call_back" -> SwipeActionItem(
                key = actionKey,
                label = "Call",
                icon = Icons.Default.Call,
                backgroundColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                isDestructive = false
            )
            "send_text" -> SwipeActionItem(
                key = actionKey,
                label = "Message",
                icon = Icons.AutoMirrored.Filled.Message,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "delete", "delete_call_log", "delete_contact" -> SwipeActionItem(
                key = actionKey,
                label = "Delete",
                icon = Icons.Default.Delete,
                backgroundColor = colorScheme.error,
                contentColor = colorScheme.onError,
                isDestructive = true
            )
            "share", "share_contact" -> SwipeActionItem(
                key = actionKey,
                label = "Share",
                icon = Icons.Default.Share,
                backgroundColor = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary,
                isDestructive = false
            )
            "select" -> SwipeActionItem(
                key = actionKey,
                label = "Select",
                icon = Icons.Default.CheckBox,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "view_contact" -> SwipeActionItem(
                key = actionKey,
                label = "View Contact",
                icon = Icons.Default.Person,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "edit_contact" -> SwipeActionItem(
                key = actionKey,
                label = "Edit",
                icon = Icons.Default.Edit,
                backgroundColor = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary,
                isDestructive = false
            )
            "copy_number" -> SwipeActionItem(
                key = actionKey,
                label = "Copy Number",
                icon = Icons.Default.ContentCopy,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "add_to_contacts" -> SwipeActionItem(
                key = actionKey,
                label = "Add Contact",
                icon = Icons.Default.PersonAdd,
                backgroundColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                isDestructive = false
            )
            "call_chat_via" -> SwipeActionItem(
                key = actionKey,
                label = "Call/Chat Via",
                icon = Icons.AutoMirrored.Filled.Chat,
                backgroundColor = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary,
                isDestructive = false
            )
            "search_truecaller" -> SwipeActionItem(
                key = actionKey,
                label = "Truecaller",
                icon = Icons.Default.Search,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "move_contact" -> SwipeActionItem(
                key = actionKey,
                label = "Move",
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            "toggle_favorite", "toggle_favourite" -> SwipeActionItem(
                key = actionKey,
                label = "Favourite",
                icon = Icons.Default.Favorite,
                backgroundColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                isDestructive = false
            )
            "block_number", "block_contact" -> SwipeActionItem(
                key = actionKey,
                label = "Block",
                icon = Icons.Default.Block,
                backgroundColor = colorScheme.error,
                contentColor = colorScheme.onError,
                isDestructive = true
            )
            "fake_call" -> SwipeActionItem(
                key = actionKey,
                label = "Fake Call",
                icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                backgroundColor = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary,
                isDestructive = false
            )
            "view_info" -> SwipeActionItem(
                key = actionKey,
                label = "Info",
                icon = Icons.Default.Info,
                backgroundColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary,
                isDestructive = false
            )
            else -> null
        }
    }
}
