import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.util.Properties

val devCycle = false

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {

    compileSdk = 30
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    signingConfigs {
        create("release") {
            val localPropFile = rootProject.file("local.properties")
            val keystoreProps = Properties().apply {
                load(localPropFile.inputStream())
            }

            keyAlias = keystoreProps["keyAlias"] as? String ?: ""
            keyPassword = keystoreProps["keyPassword"] as? String ?: ""
            storeFile = file(keystoreProps["storeFile"] as? String ?: "/")
            storePassword = keystoreProps["storePassword"] as? String ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.androidvip.sysctlgui"
        minSdk = 19
        targetSdk = 30
        versionCode = 10
        versionName = "1.1.0"
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations.addAll(listOf("en", "de", "pt-rBR"))
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.incremental" to "true",
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = !devCycle
            isShrinkResources = !devCycle
            isDebuggable = devCycle
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-kt.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    sourceSets {
        maybeCreate("main").java.srcDir("src/main/kotlin")
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    packagingOptions {
        exclude("/META-INF/**")
        exclude("/androidsupportmultidexversion.txt")
        exclude("/kotlin/**")
        exclude("/kotlinx/**")
        exclude("/okhttp3/**")
        exclude("/*.txt")
        exclude("/*.bin")
    }
}

android.applicationVariants.forEach { variant ->
    val defaultConfig = android.defaultConfig
    variant.outputs.all {
        var fileName = "sysctlgui"
        fileName += "-v${defaultConfig.versionName}(${defaultConfig.versionCode})"
        fileName += if (variant.buildType.name == "release") ".apk" else "-SNAPSHOT.apk"

        outputFile.renameTo(File(outputFile.path, fileName))
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    implementation("io.insert-koin:koin-android:3.1.3")

    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.room:room-ktx:2.3.0")
    implementation("androidx.room:room-runtime:2.3.0")

    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("com.github.topjohnwu.libsu:core:2.5.1")

    kapt("androidx.room:room-compiler:2.3.0")
}
