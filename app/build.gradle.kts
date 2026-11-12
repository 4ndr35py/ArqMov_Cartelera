import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// La API key de TMDB va en local.properties (ignorado por git), nunca en el
// codigo: TMDB_API_KEY=...  Sin clave la app funciona con los datos del cine.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val claveTmdb: String = propiedadesLocales.getProperty("TMDB_API_KEY", "").trim()

android {
    namespace = "com.example.cartelera"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.cartelera"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2-tp7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TMDB_API_KEY", "\"$claveTmdb\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)

    // Firebase: la BoM fija versiones compatibles entre si.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)      // TP7: avisos push (FCM)

    // TP7: recordatorio local programado (reemplaza a Cloud Functions en el plan Spark)
    implementation(libs.androidx.work.runtime.ktx)

    // TP7: API REST de TMDB
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // TP7: carga y cache de los afiches
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
