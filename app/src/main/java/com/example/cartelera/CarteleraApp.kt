package com.example.cartelera

import android.app.Application
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.EmuladorFirebase
import com.example.cartelera.data.avisos.Notificador

class CarteleraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EmuladorFirebase.conectarSiCorresponde()
        Metricas.inicializar(this)
        Notificador.crearCanal(this)
    }
}
