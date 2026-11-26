package com.coolappstore.evercallrecorder.by.svhp;

import android.os.ParcelFileDescriptor;
import com.coolappstore.evercallrecorder.by.svhp.ILogCallback;

interface IShellService {
    ParcelFileDescriptor startRecording(
        String audioSource,
        String audioCodec,
        int audioBitRate,
        String serverPath,
        boolean isDebuggingModeEnabled, // For debugging purposes, if true, the service will log additional information and change some logging behavior.
        ILogCallback appLoggerCallback
    ) = 1;

    void stopRecording() = 2;

    boolean isRecording() = 3;

    /**
     * Grants an AppOp permission at the package level for a given user profile, using the
     * elevated shell/root identity this service runs under. Used to grant MANAGE_ONGOING_CALLS
     * so the app's InCallService-based call detection mode is accepted by the Telecom framework.
     */
    boolean grantAppOpByPackage(String packageName, String opName, int userProfileId) = 4;

    /**
     * Grants a role to a package for a specific user profile (e.g. a companion-device role).
     * Used as a fallback for [grantAppOpByPackage] on OEM ROMs (Vivo, Oppo, Xiaomi, etc.) whose
     * aggressive permission management silently blocks direct `appops set` calls: granting the
     * app a companion-device role causes the OS to also grant MANAGE_ONGOING_CALLS as part of
     * that role's permissions on those ROMs.
     * See: https://github.com/kitsumed/ShizuCallRecorder/issues/41
     */
    boolean grantRole(String packageName, String roleName, int userProfileId) = 5;

    // The special Shizuku transaction code for "destroy" process
    void destroy() = 16777114;
}
