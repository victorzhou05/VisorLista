package com.acutecoder.visoreducativo

import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.acutecoder.visoreducativo.utils.setFullscreen

open class BaseActivity : AppCompatActivity() {

    // Tiempo máximo de inactividad permitido
    protected open val inactivityTimeout: Long = 45 * 1000 // 45 segundos (el tiempo es en milis)
    // Indica los segundos antes del final para mostrar el aviso
    private val warningTime: Long = 15 * 1000

    private val handler = Handler(Looper.getMainLooper())
    // Comprobar si el mensaje ha sido ya mostrado
    private var warningDialogShown = false
    private var warningDialog: AlertDialog? = null


    // Tarea que se ejecuta cuando se alcanza el tiempo del aviso
    private val warningRunnable = Runnable {
        showInactivityWarning()
    }

    // Tarea que se ejecuta si se ha acabo el tiempo sin haber interactuado
    private val timeoutRunnable = Runnable {
        warningDialog?.let {
            if (it.isShowing) {
                it.dismiss()
                warningDialog = null
            }
        }
        onInactivityTimeout()
    }

    // Interacción con la pantalla
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    // Cuando vuelve a estar en primer plano (por ejemplo volver de otra aplicación)
    override fun onResume() {
        super.onResume()
        resetInactivityTimer()
    }

    // Cuando está en segundo plano se queda parada, para evitar ejecuciones innecesarias
    override fun onPause() {
        super.onPause()
        stopInactivityTimer()
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(warningRunnable)

        warningDialogShown = false

        handler.postDelayed(warningRunnable, inactivityTimeout - warningTime)
        handler.postDelayed(timeoutRunnable, inactivityTimeout)
    }

    // Detiene cualquier tarea pendiente del handler (para evitar problemas (fugas o comportamientos no deseados))
    private fun stopInactivityTimer() {
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(warningRunnable)
    }

    // Diálogo de alerta
    private fun showInactivityWarning() {
        if (warningDialogShown) return

        if (!isFinishing && !isDestroyed) {
            warningDialogShown = true
            warningDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.pregunta_actividad_titulo))
                .setMessage(getString(R.string.pregunta_actividad))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.mantener)) { dialog, _ ->
                    dialog.dismiss()
                    warningDialog = null
                    resetInactivityTimer()
                }
                .setNegativeButton(getString(R.string.finalizar)) { _, _ ->
                    warningDialog = null
                    onInactivityTimeout()
                }
                .create()
            warningDialog?.setOnShowListener {
                warningDialog?.setFullscreen(true)
            }
            warningDialog?.show()
        }
    }

    protected open fun onInactivityTimeout() {
        val intent = Intent(this, InactivoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInactivityTimer()
        handler.removeCallbacksAndMessages(null)
        warningDialog?.dismiss()
        warningDialog = null
    }

}
