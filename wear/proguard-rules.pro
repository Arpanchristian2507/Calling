# R8 release: smaller APK/AAB; Play Console deobfuscates crashes with mapping.txt from the bundle.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn kotlin.**

# Wearable + GMS
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.**

# Guava (used by Wear / tiles tooling paths)
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**

# Compose / AndroidX consumer rules are merged from dependencies.
