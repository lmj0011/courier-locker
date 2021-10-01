// Top-level build file where you can add configuration options common to all sub-projects/modules.


buildscript {
    val kotlinVersionProp: String by project
    val navigationVersionProp: String by project

    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("libs")
        }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersionProp")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navigationVersionProp")
        classpath("com.google.gms:google-services:4.3.8")  // Google Services plugin
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.7.0")  // Crashlytics plugin
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() {
            content {
                includeModule("com.android.volley", "volley")
            }
        }
        maven(url = "https://jitpack.io")
        
    }
}

tasks.create("clean") {
    delete(rootProject.buildDir)
}


