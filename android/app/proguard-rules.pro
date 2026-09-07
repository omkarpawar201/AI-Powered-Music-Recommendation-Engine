# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep MediaNotificationListenerService
-keep class com.musicengine.mediapoc.service.MediaNotificationListenerService { *; }

# Keep data classes used with serialization
-keep class com.musicengine.mediapoc.model.** { *; }

# Compose
-dontwarn androidx.compose.**
