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

    // The special Shizuku transaction code for "destroy" process
    void destroy() = 16777114;
}
