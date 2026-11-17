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
// Pruebas locales con el Firebase Emulator Suite (solo debug).
val usarEmulador: Boolean = propiedadesLocales.getProperty("USAR_EMULADOR_FIREBASE", "false").trim() == "true"
// TP4: firma de release. keystore.properties NO se versiona (ver .gitignore) y
// contiene las rutas y contraseñas del almacen de claves.
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

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

    signingConfigs {
        create("release") {
            if (propiedadesFirma.containsKey("storeFile")) {
                storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USAR_EMULADOR_FIREBASE", usarEmulador.toString())
        }
        release {
            buildConfigField("boolean", "USAR_EMULADOR_FIREBASE", "false")
            optimization {
                enable = false
            }
            if (propiedadesFirma.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.fragment.ktx)
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

    // TP4: donaciones in-app (Google Play Billing)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
