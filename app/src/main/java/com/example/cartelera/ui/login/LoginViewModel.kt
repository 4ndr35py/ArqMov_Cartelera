package com.example.cartelera.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cartelera.R
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthException
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.data.ErrorAuth
import com.example.cartelera.data.MensajeriaRepository
import com.example.cartelera.data.Usuario
import com.example.cartelera.util.Validador

sealed class EstadoLogin {
    data object Inactivo : EstadoLogin()
    data object Cargando : EstadoLogin()
    data object Exito : EstadoLogin()
    /** El mensaje es un recurso de strings.xml, no texto incrustado. */
    data class Error(val mensaje: Int) : EstadoLogin()
}

/**
 * Desde el TP7 el login no es la puerta de entrada: se abre solo cuando el
 * usuario quiere marcar una funcion. «Crear cuenta» vincula la sesion anonima.
 */
class LoginViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<EstadoLogin>(EstadoLogin.Inactivo)
    val estado: LiveData<EstadoLogin> = _estado

    fun ingresar(email: String, clave: String) = ejecutar(email, clave, nuevo = false)

    fun registrar(email: String, clave: String) = ejecutar(email, clave, nuevo = true)

    private fun ejecutar(email: String, clave: String, nuevo: Boolean) {
        Validador.validar(email, clave)?.let {
            _estado.value = EstadoLogin.Error(mensajeDeValidacion(it))
            return
        }
        _estado.value = EstadoLogin.Cargando
        val alTerminar: (Result<Usuario>) -> Unit = { r ->
            r.onSuccess { usuario ->
                Metricas.identificarUsuario(usuario.uid)
                if (nuevo) Metricas.registro() else Metricas.ingreso()
                MensajeriaRepository(repo).registrarToken()
                _estado.value = EstadoLogin.Exito
            }.onFailure { e ->
                val (mensaje, tipo) = traducir(e)
                Metricas.errorAuth(tipo)
                _estado.value = EstadoLogin.Error(mensaje)
            }
        }
        if (nuevo) repo.registrar(email.trim(), clave, alTerminar)
        else repo.ingresar(email.trim(), clave, alTerminar)
    }

    private fun mensajeDeValidacion(e: Validador.Error): Int = when (e) {
        Validador.Error.EMAIL_VACIO -> R.string.error_email_vacio
        Validador.Error.EMAIL_INVALIDO -> R.string.error_email_invalido
        Validador.Error.CLAVE_VACIA -> R.string.error_clave_vacia
        Validador.Error.CLAVE_CORTA -> R.string.error_clave_corta
    }

    /** Traduce el error del repositorio a un mensaje y a un tipo para Analytics. */
    private fun traducir(e: Throwable): Pair<Int, String> =
        when ((e as? AuthException)?.tipo ?: ErrorAuth.OTRO) {
            ErrorAuth.CLAVE_DEBIL -> R.string.error_clave_corta to "clave_debil"
            ErrorAuth.EMAIL_EN_USO -> R.string.error_email_en_uso to "email_en_uso"
            ErrorAuth.CREDENCIALES -> R.string.error_credenciales to "credenciales"
            ErrorAuth.RED -> R.string.error_red to "red"
            ErrorAuth.OTRO -> R.string.error_generico to "otro"
        }
}
