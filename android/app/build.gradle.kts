plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sin clave se usa el Firebase Emulator del PC (proyecto demo). Con clave, el proyecto real.
fun config(name: String) = (project.findProperty(name) as String?) ?: System.getenv(name) ?: ""
val firebaseApiKey = config("FIREBASE_API_KEY")
val useEmulator = firebaseApiKey.isEmpty()

android {
    namespace = "com.santilega.scrollparty"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.santilega.scrollparty"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "USE_EMULATOR", "$useEmulator")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebaseApiKey.ifEmpty { "demo-key" }}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${config("FIREBASE_PROJECT_ID").ifEmpty { "demo-scrollparty" }}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${config("FIREBASE_APP_ID").ifEmpty { "1:1:android:1" }}\"")
        manifestPlaceholders["cleartext"] = useEmulator.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
