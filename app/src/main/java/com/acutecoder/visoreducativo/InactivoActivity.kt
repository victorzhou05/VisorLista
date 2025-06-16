package com.acutecoder.visoreducativo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.acutecoder.visoreducativo.utils.setFullscreen


class InactivoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inactivo)

        setFullscreen(true)

        // Cambiamos a que el click en cualquier parte de la pantalla vuelva a MainActivity (pantalla raíz)
        findViewById<View>(android.R.id.content).setOnClickListener {
            goToHome()
        }
    }

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(
               Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
