package com.acutecoder.pdfviewerdemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Función de extensión para Activity que obtiene el path y el nombre del archivo
 * desde el intent, ya sea por ACTION_VIEW o por extras ("filePath", "fileUrl", etc.)
 *
 * @return Pair con filePath y fileName, o null si no se puede obtener
 */
fun Activity.getDataFromIntent(): Pair<String, String>? {
    val filePath: String
    val fileName: String

    // Si la acción del intent es VIEW (por ejemplo, abrir un PDF desde otra app)
    if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
        filePath = intent.data.toString()
        fileName = intent.data!!.getFileName(this)
    } else {
        // Si viene desde un intent con extras ("filePath", "fileUrl", "fileUri")
        filePath = intent.extras?.getString("filePath")
            ?: intent.extras?.getString("fileUrl")
                    ?: intent.extras?.getString("fileUri")
                    ?: return null  // Si no hay ningún dato, devuelve null

        // Intenta obtener el nombre a partir del URI o del extra "fileName"
        fileName = intent.extras?.getString("fileUri")
            ?.let { uri -> Uri.parse(uri).getFileName(this) }
            ?: intent.extras?.getString("fileName") ?: ""
    }

    return filePath to fileName
}

/**
 * Función de extensión para Uri que obtiene el nombre del archivo
 * usando un ContentResolver sobre la propiedad DISPLAY_NAME.
 *
 * @return Nombre del archivo o "UNKNOWN" si no se pudo obtener
 */
fun Uri.getFileName(context: Context): String {
    var name = "UNKNOWN"

    // Cursor sobre el URI para obtener metadatos
    val cursor: Cursor? = context.contentResolver.query(
        this,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )

    // Se asegura de cerrar el cursor al finalizar
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }

    return name
}

/**
 * Función de extensión para Activity que configura la pantalla completa (fullscreen)
 * usando WindowInsetsControllerCompat.
 *
 * @param fullscreen Booleano que indica si se debe activar el modo fullscreen
 */
fun Activity.setFullscreen(fullscreen: Boolean) {
    val window = window ?: return
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)

    insetsController.apply {
        systemBarsBehavior = if (fullscreen) {
            // Oculta las barras de estado y navegación, permitiendo que aparezcan con swipe
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Muestra las barras si no se desea fullscreen
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}

/**
 * Función de extensión para Context que muestra un Toast corto
 *
 * @param msg Mensaje a mostrar
 */
fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
