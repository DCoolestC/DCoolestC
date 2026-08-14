// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    // Push notifications (Firebase). Declared here so the classpath is
    // resolvable; only actually applied in app/build.gradle.kts once
    // google-services.json exists — see the comment there.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
