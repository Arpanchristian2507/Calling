# Add project specific ProGuard rules here.
# R8: shrink + obfuscate release builds. Stack traces in Play Vitals are deobfuscated
# using mapping.txt (bundled in the AAB for modern AGP; Play applies it automatically).

# Keep line numbers for readable crash reports (file names are renamed to "SourceFile").
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Google Play Services — Wearable Data Layer (messages, capabilities, listeners)
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.**

# Material / AndroidX (consumer rules usually merged; add if you see missing class warnings)
