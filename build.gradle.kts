plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 trae Kotlin integrado (2.2). Las librerias de Firebase BoM 35 estan
    // compiladas con Kotlin 2.4, asi que subimos el compilador por esta via.
    alias(libs.plugins.kotlin.android) apply false
    // TP7: kotlinx.serialization para las respuestas de TMDB.
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
}
