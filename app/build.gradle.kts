import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose") // Required for Compose in Kotlin 2.0+
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "com.Otter.app"
    compileSdk = 36 // Android 16 (Baklava)

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties: Properties? =
        if (keystorePropertiesFile.exists()) {
            Properties().apply {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
        } else {
            null
        }

    defaultConfig {
        applicationId = "com.Otter.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "2.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        val otterBaseUrl =
            (project.findProperty("Otter_BACKEND_BASE_URL") as? String)
                ?: System.getenv("Otter_BACKEND_BASE_URL")
                ?: ""
        buildConfigField("String", "Otter_BACKEND_BASE_URL", "\"$otterBaseUrl\"")

        val otterApiKey =
            (project.findProperty("Otter_APP_API_KEY") as? String)
                ?: System.getenv("Otter_APP_API_KEY")
        buildConfigField("String", "Otter_APP_API_KEY", "\"${otterApiKey ?: ""}\"")
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // composeOptions removed: Managed by 'org.jetbrains.kotlin.plugin.compose' in Kotlin 2.0+

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        freeCompilerArgs +=
            listOf(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-Xannotation-default-target=param-property",
            )
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    bundle {
        language { enableSplit = false }
        density { enableSplit = false }
    }

    lint {
        disable.add("PropertyEscape")
    }

    // --- NEW: Rename Build Output ---
    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abi = output?.getFilter(com.android.build.OutputFile.ABI) ?: "universal"
            output?.outputFileName = "Otter-$abi-${buildType.name}.apk"
        }
    }
}

dependencies {
    // Core Android & Lifecycle (Latest Feb 2026)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose (BOM 2025.01.01)
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha14")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room (Stable 2.7.0)
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // Hilt DI (Stable 2.52)
    implementation("com.google.dagger:hilt-android:2.59.1")
    ksp("com.google.dagger:hilt-compiler:2.59.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")

    // Coil 3.0 (Now Multiplatform support)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")

    // Ktor Client for Coil
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // ExoPlayer (Media3 1.5.0)
    val media3Version = "1.5.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    implementation("androidx.media:media:1.7.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Core library desugaring (required by NewPipeExtractor for minSdk < 33)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")

    // 3rd Party
    implementation("io.github.junkfood02.youtubedl-android:library:0.17.3")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.3")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.25.2")
    implementation("com.google.android.material:material:1.13.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

detekt {
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = true
}

ktlint {
    version.set("1.2.1")
    android = true
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
