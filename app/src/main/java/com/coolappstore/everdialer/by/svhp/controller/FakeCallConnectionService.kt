package com.coolappstore.everdialer.by.svhp.controller

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Powers the "Fake Call" feature by registering a self-managed [PhoneAccount] and placing a
 * genuine Telecom incoming call through it.
 *
 * Because this app is the default dialer and its [CallService] (an [android.telecom.InCallService])
 * declares the `android.telecom.IN_CALL_SERVICE_UI` capability, Telecom routes this call straight
 * into the exact same [CallService] → [com.coolappstore.everdialer.by.svhp.view.screen.CallActivity]
 * pipeline used for real calls. No custom incoming-call screen or notification is built — the fake
 * call is indistinguishable from a real one in every screen and every notification.
 */
class FakeCallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val address = request?.address
        val name = request?.extras?.getString(EXTRA_FAKE_NAME)
            ?: address?.schemeSpecificPart
            ?: "Unknown"

        val connection = FakeConnection()
        connection.setConnectionCapabilities(Connection.CAPABILITY_MUTE or Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD)
        connection.audioModeIsVoip = true
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    companion object {
        const val EXTRA_FAKE_NAME = "com.coolappstore.everdialer.by.svhp.FAKE_NAME"
        const val EXTRA_FAKE_ID   = "com.coolappstore.everdialer.by.svhp.FAKE_ID"
        private const val ACCOUNT_ID = "ever_dialer_fake_call_account"

        private fun phoneAccountHandle(context: Context): PhoneAccountHandle =
            PhoneAccountHandle(ComponentName(context, FakeCallConnectionService::class.java), ACCOUNT_ID)

        /** Registers the self-managed PhoneAccount used to power Fake Call. Safe to call repeatedly. */
        fun ensureRegistered(context: Context) {
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val handle = phoneAccountHandle(context)
                val account = PhoneAccount.builder(handle, "Ever Dialer Fake Call")
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                    .setShortDescription("Used to simulate incoming calls")
                    .build()
                tm.registerPhoneAccount(account)
            } catch (_: Exception) {}
        }

        /**
         * Places a simulated incoming call. This routes through real Telecom + the app's real
         * [CallService] and [com.coolappstore.everdialer.by.svhp.view.screen.CallActivity] —
         * the exact same incoming-call screen, system notification, and ongoing-call screen used
         * for genuine calls. No actual call is placed to [number].
         */
        fun placeFakeIncomingCall(context: Context, id: String, displayName: String, number: String) {
            ensureRegistered(context)
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val handle = phoneAccountHandle(context)
                val extras = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, Uri.fromParts("tel", number, null))
                    putString(EXTRA_FAKE_NAME, displayName)
                    putString(EXTRA_FAKE_ID, id)
                }
                tm.addNewIncomingCall(handle, extras)
            } catch (_: Exception) {}
        }
    }
}

/** A [Connection] with no real audio/network backing — just enough to drive the real call UI. */
class FakeConnection : Connection() {

    override fun onAnswer(videoState: Int) {
        super.onAnswer(videoState)
        setActive()
    }

    override fun onReject() {
        super.onReject()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        super.onAbort()
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    override fun onHold() {
        super.onHold()
        setOnHold()
    }

    override fun onUnhold() {
        super.onUnhold()
        setActive()
    }

    override fun onShowIncomingCallUi() {
        // No-op: the default dialer's InCallService (CallService → CallActivity) already
        // provides the real incoming-call UI for this self-managed connection.
    }
}
