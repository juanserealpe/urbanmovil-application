import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "co.urbanmovil.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "co.urbanmovil.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Backend de UrbanMovil. Por defecto, el «localhost» del PC visto desde el emulador.
        // Teléfono físico: ./gradlew installDebug -Purbanmovil.apiUrl=http://<IP-del-PC>:8080
        // Vacío (-Purbanmovil.apiUrl=) = prototipo sin servidor, con datos quemados.
        val urlApi = providers.gradleProperty("urbanmovil.apiUrl").getOrElse("http://10.0.2.2:8080")
        buildConfigField("String", "API_URL", "\"$urlApi\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            // El backend local va por HTTP plano; solo se permite en debug.
            manifestPlaceholders["traficoSinCifrar"] = "true"
        }
        release {
            manifestPlaceholders["traficoSinCifrar"] = "false"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // AGP 9 lo trae apagado; se usa para pasar BuildConfig.DEBUG a la UI compartida (esDebug).
        buildConfig = true
    }
}