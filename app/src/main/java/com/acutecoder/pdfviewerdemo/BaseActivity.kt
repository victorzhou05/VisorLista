package com.acutecoder.pdfviewerdemo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    protected open val inactivityTimeout: Long = 45 * 1000
    private val warningTime: Long = 15 * 1000 // 10 segundos antes del timeout

    private val handler = Handler(Looper.getMainLooper())
    private var warningDialogShown = false
    private var warningDialog: AlertDialog? = null

    private val warningRunnable = Runnable {
        showInactivityWarning()
    }

    private val timeoutRunnable = Runnable {
        // Si el diálogo está mostrado, simula que se pulsó "No"
        warningDialog?.let {
            if (it.isShowing) {
                it.dismiss()
                warningDialog = null
            }
        }
        onInactivityTimeout()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    protected fun registerUserInteraction() {
        resetInactivityTimer()
    }

    override fun onResume() {
        super.onResume()
        resetInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        stopInactivityTimer()
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(warningRunnable)

        warningDialogShown = false

        // El aviso se lanza 10 segundos antes del timeout
        handler.postDelayed(warningRunnable, inactivityTimeout - warningTime)
        handler.postDelayed(timeoutRunnable, inactivityTimeout)
    }

    private fun stopInactivityTimer() {
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(warningRunnable)
    }

    private fun showInactivityWarning() {
        if (warningDialogShown) return
        warningDialogShown = true

        if (!isFinishing && !isDestroyed) {
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
                .show()
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
