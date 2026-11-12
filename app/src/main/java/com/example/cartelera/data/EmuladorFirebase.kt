package com.example.cartelera.data

import com.example.cartelera.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Pruebas con el Firebase Emulator Suite (Auth y Firestore corriendo en la PC).
 * Solo se activa en el build de debug y si local.properties tiene
 * USAR_EMULADOR_FIREBASE=true. Debe llamarse antes de cualquier otro uso de
 * Firebase (CarteleraApp.onCreate).
 */
object EmuladorFirebase {

    /** Desde el emulador de Android, 10.0.2.2 es la PC (localhost del anfitrion). */
    private const val HOST = "10.0.2.2"
    private const val PUERTO_AUTH = 9099
    private const val PUERTO_FIRESTORE = 8080

    fun conectarSiCorresponde() {
        if (!BuildConfig.USAR_EMULADOR_FIREBASE) return
        FirebaseAuth.getInstance().useEmulator(HOST, PUERTO_AUTH)
        FirebaseFirestore.getInstance().useEmulator(HOST, PUERTO_FIRESTORE)
    }
}
