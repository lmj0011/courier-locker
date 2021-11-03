import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val kotlinVersionProp: String by project
val navigationVersionProp: String by project
val fragmentVersionProp: String by project
val roomVersionProp: String by project
val lifecycleVersionProp: String by project
val coroutineVersionProp: String by project
val pagingVersionProp: String by project
val fuelVersionProp: String by project
val googleMapsApiKeyDebugProp: String by project
val googleMapsApiKeyReleaseProp: String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = 31
    ndkVersion = "21.3.6528147"

    defaultConfig {
        applicationId = "name.lmj0011.courierlocker"
        minSdk = 26
        targetSdk =31
        versionCode = 99
        versionName = "2.3.0-beta01"

        vectorDrawables {
            useSupportLibrary = true
        }

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ref: https://stackoverflow.com/a/48674264/2445763
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            resValue("string", "google_maps_key", googleMapsApiKeyReleaseProp)
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            ndk {
                debugSymbolLevel = "FULL"
            }
        }

        named("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            resValue("string", "google_maps_key", googleMapsApiKeyDebugProp)
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
    }

    buildTypes.forEach {
        it.resValue("string", "app_build", getGitSha().take(8))
//        it.resValue("string", "git_commit_count", getCommitCount())
//        it.resValue("string", "git_commit_sha", getGitSha())
//        it.resValue("string", "app_buildtime", getBuildTime())
    }

    flavorDimensions("default")

    productFlavors {
        create("dev") { // active development/testing
            dimension = "default"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "Courier Locker (dev)")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_dev"
            manifestPlaceholders["appRoundIcon"] = "@mipmap/ic_launcher_dev_round"
        }

        create("prod") { // official release
            dimension = "default"
            applicationIdSuffix = ".prod"
            resValue("string", "app_name", "Courier Locker")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appRoundIcon"] = "@mipmap/ic_launcher_round"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lint {
        isAbortOnError = true
    }

    // Big Up! to cSn: https://stackoverflow.com/a/36626584/2445763
    configurations.implementation {
        isCanBeResolved = true
    }

    configurations.androidTestImplementation {
        isCanBeResolved = true
    }

    project.gradle.addBuildListener(object : BuildListener {
        override fun settingsEvaluated(settings: Settings) {}

        override fun projectsLoaded(gradle: Gradle) {}

        override fun projectsEvaluated(gradle: Gradle) {}

        override fun buildFinished(result: BuildResult) {
            var str = "# auto-generated; this file should be checked into version control\n"
            val resolvedImplementationConfig = configurations.implementation.get().resolvedConfiguration
            val resolvedAndroidTestImplementationConfig = configurations.androidTestImplementation.get().resolvedConfiguration
            val fileName = "deps.list.txt"
            val depsFile = File(projectDir, fileName)

            resolvedImplementationConfig.firstLevelModuleDependencies.forEach { dep ->
                str += "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}\n"
            }

            resolvedAndroidTestImplementationConfig.firstLevelModuleDependencies.forEach { dep ->
                str += "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}\n"
            }

            GlobalScope.launch(Dispatchers.IO) {
                depsFile.writeText(str)
                println("\n${fileName} created.\n")
            }
        }
    })
}

dependencies {
    val fTree = fileTree("lib")
    fTree.include("*.jar", "*.aar")

    implementation(fTree)
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersionProp")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersionProp")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersionProp")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.5.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersionProp")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")


    // Room dependencies
    implementation("androidx.room:room-runtime:$roomVersionProp")
    annotationProcessor("androidx.room:room-compiler:$roomVersionProp")
    kapt("androidx.room:room-compiler:$roomVersionProp")

    // GSON
    implementation("com.google.code.gson:gson:2.8.7")

    // Lifecycle-aware components
    // ref: https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:$lifecycleVersionProp")

    // useful for testing with mock data
    implementation("com.github.moove-it:fakeit:v0.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersionProp")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersionProp")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.7.0")

    // HTTP lib
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersionProp")
    implementation("com.github.kittinunf.fuel:fuel-json:$fuelVersionProp")

    // androidx.preference
    implementation("androidx.preference:preference-ktx:1.1.1")

    // An adaptation of the JSR-310 backport for Android.
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")

    // Firebase SDK
    implementation("com.google.firebase:firebase-crashlytics:18.0.1")
    implementation("com.google.firebase:firebase-analytics:19.0.0")

    // Maps SDK and FusedLocationProviderClient dependencies
    implementation("com.google.android.gms:play-services-basement:17.6.0")
    implementation("com.google.android.gms:play-services-base:17.6.0")
    implementation("com.google.android.gms:play-services-gcm:17.0.0")
    implementation("com.google.android.gms:play-services-location:18.0.0")

    // Kotson: Gson for Kotlin
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    // Maps SDK for Android Utility Library
    // https://developers.google.com/maps/documentation/android-sdk/v3-client-migration#import-the-beta-compatible-utility-library
    implementation("com.google.android.libraries.maps:maps:3.1.0-beta")
    implementation("com.google.maps.android:android-maps-utils-v3:2.2.3")

    // dependency injection
    implementation("org.kodein.di:kodein-di:7.6.0")

    // sqlite
    implementation("com.github.requery:sqlite-android:3.35.5")

    // https://github.com/Zhuinden/livedata-combinetuple-kt#livedata-combinetuple-kt
    implementation("com.github.Zhuinden:livedata-combinetuple-kt:1.2.1")

    // KeyboardVisibilityEvent: https://github.com/yshrsmz/KeyboardVisibilityEvent/releases
    implementation("net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:3.0.0-RC3")
}

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
// ref: https://github.com/tachiyomiorg/tachiyomi/blob/master/app/build.gradle.kts
fun getCommitCount(): String {
    return runCommand("git rev-list --count HEAD")
}

fun getGitSha(): String {
    return runCommand("git rev-parse HEAD")
}

fun getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}
