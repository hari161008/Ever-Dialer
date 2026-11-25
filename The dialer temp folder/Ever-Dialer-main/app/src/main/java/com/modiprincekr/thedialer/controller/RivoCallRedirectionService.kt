package com.modiprincekr.thedialer.controller

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle

/**
 * Not registered in AndroidManifest.xml anymore — The Dialer no longer chooses the SIM for
 * outgoing calls itself, so this always passes the call through untouched and defers entirely to
 * Android's own SIM selection (Settings → Network & internet → SIMs → Calls). Kept only so stale
 * local project checkouts that still reference this class don't fail to compile; safe to delete
 * this file manually.
 */
class RivoCallRedirectionService : CallRedirectionService() {
    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        placeCallUnmodified()
    }
}
