package com.example.cartelera.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.cartelera.data.donacion.Donaciones

/** Resultado de una donacion, ya traducido a algo que la UI puede mostrar. */
sealed interface ResultadoDonacion {
    /** La compra se confirmo y se consumio. */
    data class Gracias(val montoUsd: Int) : ResultadoDonacion
    /** El usuario cerro la hoja de compra sin pagar. */
    data object Cancelada : ResultadoDonacion
    /** Play la acepto pero queda pendiente de pago (por ejemplo, efectivo). */
    data object Pendiente : ResultadoDonacion
    /** Cualquier otro error de Billing; el codigo es el de BillingResponseCode. */
    data class Fallo(val codigo: Int) : ResultadoDonacion
}

/**
 * Unico punto de acceso a Google Play Billing (TP4).
 *
 * Encapsula BillingClient para que ni la Activity ni el ViewModel importen
 * clases de Billing (misma regla que Firebase, Retrofit y WorkManager).
 * Las donaciones son consumibles: cada compra se consume al confirmarse para
 * que el usuario pueda volver a donar. "Restaurar" al iniciar consiste en
 * consumir las compras que quedaron sin consumir (por un corte de red o una
 * app cerrada durante el pago), para que no queden bloqueadas.
 */
class BillingRepository(
    context: Context,
    private val alResultado: (ResultadoDonacion) -> Unit
) : PurchasesUpdatedListener {

    private val cliente: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var detalles: Map<String, ProductDetails> = emptyMap()

    /** Abre la conexion con Play. onListo recibe false si Play no esta disponible. */
    fun conectar(onListo: (Boolean) -> Unit) {
        if (cliente.isReady) {
            onListo(true)
            return
        }
        cliente.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultado: BillingResult) {
                val listo = resultado.responseCode == BillingClient.BillingResponseCode.OK
                if (listo) consumirPendientes()
                onListo(listo)
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Trae precio y descripcion de los productos de donacion desde Play. */
    fun consultarProductos(onResultado: (Result<List<ProductDetails>>) -> Unit) {
        val productos = Donaciones.idsProducto.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productos).build()
        cliente.queryProductDetailsAsync(params) { resultado, respuesta ->
            if (resultado.responseCode != BillingClient.BillingResponseCode.OK) {
                onResultado(Result.failure(IllegalStateException("billing ${resultado.responseCode}")))
                return@queryProductDetailsAsync
            }
            detalles = respuesta.productDetailsList.associateBy { it.productId }
            onResultado(Result.success(respuesta.productDetailsList))
        }
    }

    /** Precio que muestra Play (con moneda local), o null si aun no se consulto. */
    fun precioFormateado(productId: String): String? =
        detalles[productId]?.oneTimePurchaseOfferDetails?.formattedPrice

    /** Abre la hoja de compra de Play para un producto de donacion. */
    fun donar(actividad: Activity, productId: String): Boolean {
        val detalle = detalles[productId] ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(detalle)
            .build()
        val flujo = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build()
        val resultado = cliente.launchBillingFlow(actividad, flujo)
        return resultado.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Llamado por Play cuando termina una compra iniciada en la hoja de compra. */
    override fun onPurchasesUpdated(resultado: BillingResult, compras: List<Purchase>?) {
        when (resultado.responseCode) {
            BillingClient.BillingResponseCode.OK -> compras.orEmpty().forEach { procesar(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> alResultado(ResultadoDonacion.Cancelada)
            else -> alResultado(ResultadoDonacion.Fallo(resultado.responseCode))
        }
    }

    /** Consume las compras pagadas que quedaron sin consumir. */
    private fun consumirPendientes() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        cliente.queryPurchasesAsync(params) { resultado, compras ->
            if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
                compras.forEach { procesar(it, silencioso = true) }
            }
        }
    }

    private fun procesar(compra: Purchase, silencioso: Boolean = false) {
        if (compra.purchaseState == Purchase.PurchaseState.PENDING) {
            if (!silencioso) alResultado(ResultadoDonacion.Pendiente)
            return
        }
        if (compra.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val monto = compra.products.firstNotNullOfOrNull { Donaciones.montoDe(it) }
        val consumo = ConsumeParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
        cliente.consumeAsync(consumo) { resultado, _ ->
            if (silencioso) return@consumeAsync
            if (resultado.responseCode == BillingClient.BillingResponseCode.OK && monto != null) {
                alResultado(ResultadoDonacion.Gracias(monto))
            } else {
                alResultado(ResultadoDonacion.Fallo(resultado.responseCode))
            }
        }
    }

    fun cerrar() {
        if (cliente.isReady) cliente.endConnection()
    }
}
