plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

// Optional stable release key, injected by CI (see README). Falls back to the debug key.
val novaKeystore = System.getenv("NOVA_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.abu00salman.novadrive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abu00salman.novadrive"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "2.4.0-beta2"
        resourceConfigurations += listOf("en", "ar")
    }

    signingConfigs {
        if (novaKeystore != null) {
            create("nova") {
                storeFile = novaKeystore
                storePassword = System.getenv("NOVA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NOVA_KEY_ALIAS")
                keyPassword = System.getenv("NOVA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (novaKeystore != null) signingConfigs.getByName("nova")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*",
                "META-INF/INDEX.LIST", "META-INF/*.kotlin_module", "META-INF/versions/9/module-info.class", "module-info.class",
            )
        }
    }
    lint { checkReleaseBuilds = false; abortOnError = false }
    // JVM screenshot tests (Robolectric + Roborazzi) render the real Compose UI to PNG in CI.
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Screenshot tests write PNGs Gradle doesn't track, so never skip or restore them from cache.
tasks.withType<Test>().configureEach {
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // Nova AI: official Anthropic SDK for Claude (OpenAI is called over plain HTTPS).
    implementation("com.anthropic:anthropic-java:3.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.40.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.40.1")
}
