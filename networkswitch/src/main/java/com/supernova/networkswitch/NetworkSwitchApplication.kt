package com.supernova.networkswitch

import android.app.Application

// Not used as the manifest Application when bundled into Ever Dialer (RivoApp is used instead).
// Kept for standalone-build compatibility; no longer Hilt-annotated since the Hilt Gradle plugin
// is incompatible with this project's AGP version.
class NetworkSwitchApplication : Application()
