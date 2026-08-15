plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Every build gets a version code one higher than the last, so a newer APK
// always installs *over* an older one instead of being rejected as a
// downgrade. CI passes the workflow run number; local builds fall back to 1.
val buildVersionCode = (project.findProperty("isokoVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.isokovibe.musicplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.isokovibe.musicplayer"
        minSdk = 26
        targetSdk = 34
        versionCode = buildVersionCode
        versionName = "0.1.$buildVersionCode"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // A checked-in signing key, used by BOTH build types.
    //
    // Android refuses to install an APK over an existing app when the two are
    // signed by different keys — it reports a signature mismatch and the only
    // way through is to uninstall first (losing playlists/favorites/settings
    // with it). Gradle's default behaviour is to auto-generate a throwaway
    // debug keystore in ~/.android on whatever machine is building, and CI
    // runners are wiped between jobs, so every single CI build used to be
    // signed by a brand-new key. Pinning the key here is what makes updates
    // install cleanly on top of each other.
    //
    // This key is deliberately committed and its password is not a secret: it
    // exists so test builds share an identity, nothing more. Publishing to the
    // Play Store needs a *separate* release key that is never committed — see
    // the "Signing & updates" section of the README before you ship.
    signingConfigs {
        create("isokovibe") {
            storeFile = file("isokovibe-debug.keystore")
            storePassword = "isokovibe"
            keyAlias = "isokovibe"
            keyPassword = "isokovibe"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("isokovibe")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("isokovibe")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 / ExoPlayer for playback + media session + background service
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("com.google.guava:guava:33.2.1-android")

    // Coil for album art loading (content:// URIs)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Accompanist permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Local persistence for favorites, playlists, and settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Periodic background check for announcements from iSokoVibe.com.ng.
    // No HTTP client dependency alongside it — the fetch is one small GET
    // via HttpURLConnection, which doesn't justify pulling in OkHttp.
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
