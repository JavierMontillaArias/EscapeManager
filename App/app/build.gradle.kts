plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.javiermontillaarias.escapemanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.javiermontillaarias.escapemanager"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://escapemanager-production.up.railway.app/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BASE_URL", "\"https://escapemanager-production.up.railway.app/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

// PR-01: Falla el build de release si la URL de producción es todavía un placeholder.
tasks.register("checkProductionConfig") {
    doLast {
        val releaseUrl = android.buildTypes.getByName("release")
            .buildConfigFields["BASE_URL"]?.value ?: ""
        if (releaseUrl.contains("tudominio")) {
            throw GradleException(
                "BASE_URL de producción contiene 'tudominio'. " +
                "Actualiza buildConfigField en build.gradle.kts antes de publicar."
            )
        }
    }
}
tasks.whenTaskAdded {
    if (name == "assembleRelease") dependsOn("checkProductionConfig")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.nav.fragment)
    implementation(libs.nav.ui)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.mlkit.barcode)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mpandroidchart)
    implementation(libs.swiperefresh)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    testImplementation("io.mockk:mockk:1.13.10")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
