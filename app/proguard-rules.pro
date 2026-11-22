# ProGuard / R8 Rules for Ever Dialer (Play Store Release)

# 1. Preserve Source file and Line numbers for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Kotlinx Serialization
# Keeps models, serializers, and companion objects so JSON caching and preferences do not break
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.coolappstore.everdialer.by.svhp.modal.data.** { *; }
-keepclassmembers class com.coolappstore.everdialer.by.svhp.modal.data.** { *; }

# 3. Koin Dependency Injection
# Prevents Koin from failing when resolving ViewModels, Singletons, or Repositories
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.coolappstore.everdialer.by.svhp.controller.**ViewModel { *; }
-keep class com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager { *; }
-keep class com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager { *; }
-keep class com.coolappstore.everdialer.by.svhp.modal.** { *; }
-keep class io.insertkoin.** { *; }

# 4. Compose Destinations
# Keeps destination classes, routes, and transition styles
-keep class * implements com.ramcosta.composedestinations.** { *; }
-keep @com.ramcosta.composedestinations.annotation.** class * { *; }
-keep class **.destinations.** { *; }
-keep class **.NavGraphs { *; }
-keep class com.coolappstore.everdialer.by.svhp.view.theme.**TransitionStyle { *; }

# 5. Telecom & Android System Entry Points
# Ensures Android Telecom and OS can always bind to InCallService and background services
-keep public class * extends android.telecom.InCallService { *; }
-keep public class * extends android.telecom.ConnectionService { *; }
-keep public class * extends android.app.Service { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }

# 6. Third Party Libraries
-keep class com.google.zxing.** { *; }
-dontwarn coil.**
-dontwarn androidx.biometric.**
