# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve debugging stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    Companion();
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation @kotlinx.serialization.Serializable class *
-keepclassmembers @kotlinx.serialization.Serializable class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# The Dialer Data Models & Repositories
-keep class com.modiprincekr.thedialer.modal.data.** { *; }
-keep class com.modiprincekr.thedialer.modal.repository.** { *; }
-keep class com.modiprincekr.thedialer.modal.interface.** { *; }

# Compose Destinations
-keep class com.ramcosta.composedestinations.** { *; }
-keep interface com.ramcosta.composedestinations.** { *; }

# Koin Dependency Injection
-keep class * extends org.koin.core.module.Module { *; }
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# R8 rules for hidden telephony APIs used by networkswitch
-dontwarn android.os.ServiceManager
-dontwarn com.android.internal.telephony.**
-keep class com.supernova.networkswitch.** { *; }
