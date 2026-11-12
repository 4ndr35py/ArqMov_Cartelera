package com.example.cartelera

import android.app.Application
import com.example.cartelera.analytics.Metricas

class CarteleraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Metricas.inicializar(this)
    }
}
