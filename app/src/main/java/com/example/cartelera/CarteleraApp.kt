package com.example.cartelera

import android.app.Application
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.avisos.Notificador

class CarteleraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Metricas.inicializar(this)
        Notificador.crearCanal(this)
    }
}
