package com.example.cartelera.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cartelera.R
import com.example.cartelera.analytics.Metricas
import com.example.cartelera.data.AuthRepository
import com.example.cartelera.util.Validador
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

sealed class EstadoLogin {
    data object Inactivo : EstadoLogin()
    data object Cargando : EstadoLogin()
    data object Exito : EstadoLogin()
    /** El mensaje es un recurso de strings.xml, no texto incrustado. */
    data class Error(val mensaje: Int) : EstadoLogin()
}

class LoginViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _estado = MutableLiveData<EstadoLogin>(EstadoLogin.Inactivo)
    val estado: LiveData<EstadoLogin> = _estado

    /** Si ya hay sesion iniciada, no se vuelve a pedir credenciales. */
    fun haySesion(): Boolean {
        val usuario = repo.usuarioActual ?: return false
        Metricas.identificarUsuario(usuario.uid)
        return true
    }

    fun ingresar(email: String, clave: String) = ejecutar(email, clave, nuevo = false)

    fun registrar(email: String, clave: String) = ejecutar(email, clave, nuevo = true)

    private fun ejecutar(email: String, clave: String, nuevo: Boolean) {
        Validador.validar(email, clave)?.let {
            _estado.value = EstadoLogin.Error(mensajeDeValidacion(it))
            return
        }
        _estado.value = EstadoLogin.Cargando
        val alTerminar: (Result<com.google.firebase.auth.FirebaseUser>) -> Unit = { r ->
            r.onSuccess { usuario ->
                Metricas.identificarUsuario(usuario.uid)
                if (nuevo) Metricas.registro() else Metricas.ingreso()
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

    /** Traduce excepciones de Firebase a un mensaje y a un tipo para Analytics. */
    private fun traducir(e: Throwable): Pair<Int, String> = when (e) {
        is FirebaseAuthWeakPasswordException -> R.string.error_clave_corta to "clave_debil"
        is FirebaseAuthUserCollisionException -> R.string.error_email_en_uso to "email_en_uso"
        is FirebaseAuthInvalidCredentialsException -> R.string.error_credenciales to "credenciales"
        is FirebaseNetworkException -> R.string.error_red to "red"
        else -> R.string.error_generico to "otro"
    }
}
