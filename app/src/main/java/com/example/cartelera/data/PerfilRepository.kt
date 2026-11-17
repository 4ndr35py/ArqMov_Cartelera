package com.example.cartelera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException

/** Pelicula favorita elegida por la persona: clave de la pelicula y titulo. */
data class Favorita(val clave: String, val titulo: String)

/**
 * Perfil guardado en el telefono, por cuenta (uid): foto, alias, descripcion, fecha de
 * nacimiento y pelicula favorita. Nada se sube a un servidor.
 *
 * La membresia no va aca: el tipo y el vencimiento vendran de Eventive.
 */
class PerfilRepository(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Foto de perfil de esta cuenta, o null si no cargo ninguna. */
    fun foto(uid: String): File? =
        prefs.getString(CLAVE_FOTO + uid, null)?.let { File(it) }?.takeIf { it.exists() }

    /**
     * Copia la foto elegida en el almacenamiento privado, reducida a unos 512 px.
     * Devuelve false si el archivo no se pudo leer o guardar.
     */
    fun guardarFoto(uid: String, origen: Uri): Boolean {
        val bitmap = decodificar(origen) ?: return false
        // Nombre nuevo cada vez: Coil no reusa la imagen vieja del cache.
        val destino = File(app.filesDir, "foto_${uid}_${System.currentTimeMillis()}.jpg")
        return try {
            destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val anterior = foto(uid)
            prefs.edit().putString(CLAVE_FOTO + uid, destino.absolutePath).apply()
            anterior?.delete()
            true
        } catch (e: IOException) {
            destino.delete()
            false
        }
    }

    /** Alias elegido por la cuenta, o null si no eligio ninguno. */
    fun alias(uid: String): String? = prefs.getString(CLAVE_ALIAS + uid, null)

    fun guardarAlias(uid: String, alias: String) {
        prefs.edit().putString(CLAVE_ALIAS + uid, alias).apply()
    }

    /** Descripcion libre del perfil, o null si no escribio ninguna. */
    fun descripcion(uid: String): String? = prefs.getString(CLAVE_DESCRIPCION + uid, null)

    fun guardarDescripcion(uid: String, texto: String) {
        prefs.edit().putString(CLAVE_DESCRIPCION + uid, texto).apply()
    }

    /** Fecha de nacimiento ("yyyy-MM-dd"), o null si no la cargo. Sirve para el precio senior o estudiante. */
    fun nacimiento(uid: String): String? = prefs.getString(CLAVE_NACIMIENTO + uid, null)

    fun guardarNacimiento(uid: String, fecha: String) {
        prefs.edit().putString(CLAVE_NACIMIENTO + uid, fecha).apply()
    }

    /** Pelicula favorita, o null si no eligio ninguna. */
    fun favorita(uid: String): Favorita? {
        val clave = prefs.getString(CLAVE_FAV_CLAVE + uid, null) ?: return null
        val titulo = prefs.getString(CLAVE_FAV_TITULO + uid, null) ?: return null
        return Favorita(clave, titulo)
    }

    fun guardarFavorita(uid: String, favorita: Favorita?) {
        val editor = prefs.edit()
        if (favorita == null) {
            editor.remove(CLAVE_FAV_CLAVE + uid).remove(CLAVE_FAV_TITULO + uid)
        } else {
            editor.putString(CLAVE_FAV_CLAVE + uid, favorita.clave).putString(CLAVE_FAV_TITULO + uid, favorita.titulo)
        }
        editor.apply()
    }

    /** Lee la imagen con una reduccion de 1/4 para no reservar memoria de mas con fotos grandes. */
    private fun decodificar(uri: Uri): Bitmap? {
        val opciones = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bruto = try {
            app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        } catch (e: IOException) {
            null
        } ?: return null
        val lado = maxOf(bruto.width, bruto.height)
        if (lado <= LADO_MAXIMO) return bruto
        val factor = LADO_MAXIMO.toFloat() / lado
        return Bitmap.createScaledBitmap(
            bruto,
            (bruto.width * factor).toInt().coerceAtLeast(1),
            (bruto.height * factor).toInt().coerceAtLeast(1),
            true
        )
    }

    private companion object {
        const val PREFS = "perfil"
        const val CLAVE_FOTO = "foto_"
        const val CLAVE_ALIAS = "alias_"
        const val CLAVE_DESCRIPCION = "descripcion_"
        const val CLAVE_NACIMIENTO = "nacimiento_"
        const val CLAVE_FAV_CLAVE = "favorita_clave_"
        const val CLAVE_FAV_TITULO = "favorita_titulo_"
        const val LADO_MAXIMO = 512
    }
}
