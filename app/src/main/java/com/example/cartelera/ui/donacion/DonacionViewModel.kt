package com.example.cartelera.ui.donacion

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.BillingRepository
import com.example.cartelera.data.ResultadoDonacion
import com.example.cartelera.data.donacion.Donaciones

/** Una opcion lista para dibujar: precio con la moneda que muestra Play. */
data class OpcionMostrada(
    val productId: String,
    val montoUsd: Int,
    val precio: String,
    val sugerida: Boolean
)

sealed class EstadoDonacion {
    data object Cargando : EstadoDonacion()
    data class Lista(val opciones: List<OpcionMostrada>) : EstadoDonacion()
    data object NoDisponible : EstadoDonacion()
}

/**
 * Logica de la pantalla "Apoyar al cine". No importa clases de Billing: habla
 * con BillingRepository y recibe resultados ya traducidos.
 */
class DonacionViewModel(app: Application) : AndroidViewModel(app) {

    private val billing = BillingRepository(app) { resultado ->
        _resultado.postValue(resultado)
    }

    private val _estado = MutableLiveData<EstadoDonacion>(EstadoDonacion.Cargando)
    val estado: LiveData<EstadoDonacion> = _estado

    private val _resultado = MutableLiveData<ResultadoDonacion?>()
    val resultado: LiveData<ResultadoDonacion?> = _resultado

    init {
        Metricas.apoyarAbrir()
        cargar()
    }

    fun cargar() {
        _estado.value = EstadoDonacion.Cargando
        billing.conectar { conectado ->
            if (!conectado) {
                _estado.postValue(EstadoDonacion.NoDisponible)
                return@conectar
            }
            billing.consultarProductos { r ->
                r.onSuccess {
                    val opciones = Donaciones.opciones.mapNotNull { opcion ->
                        billing.precioFormateado(opcion.productId)?.let { precio ->
                            OpcionMostrada(
                                productId = opcion.productId,
                                montoUsd = opcion.montoUsd,
                                precio = precio,
                                sugerida = opcion.montoUsd == Donaciones.MONTO_SUGERIDO_USD
                            )
                        }
                    }
                    _estado.postValue(
                        if (opciones.isEmpty()) EstadoDonacion.NoDisponible
                        else EstadoDonacion.Lista(opciones)
                    )
                }.onFailure {
                    _estado.postValue(EstadoDonacion.NoDisponible)
                }
            }
        }
    }

    /** Abre la hoja de compra de Play. Devuelve false si el producto no esta disponible. */
    fun donar(actividad: Activity, opcion: OpcionMostrada): Boolean {
        val abierta = billing.donar(actividad, opcion.productId)
        return abierta
    }

    /** Evita que un resultado viejo se muestre de nuevo al girar la pantalla. */
    fun resultadoMostrado() {
        _resultado.value = null
    }

    override fun onCleared() {
        billing.cerrar()
        super.onCleared()
    }
}
