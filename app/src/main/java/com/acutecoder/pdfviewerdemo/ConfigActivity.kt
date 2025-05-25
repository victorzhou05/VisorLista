package com.acutecoder.pdfviewerdemo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class ConfigActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private lateinit var btnSeleccionarJson: Button
    private lateinit var jsonFilePath: TextView

    // Preferencias para almacenar ruta al JSON cacheado
    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_PATH = "jsonFilePath"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        pref = getSharedPreferences("pref", MODE_PRIVATE)
        val cachedJsonPath = pref.getString(PREF_KEY_JSON_PATH, null)

        btnSeleccionarJson = findViewById(R.id.btnElegirJson3)
        jsonFilePath = findViewById(R.id.rutaJson)

        // Si hay un JSON cacheado, lo mostramos
        if (cachedJsonPath != null) {
            jsonFilePath.text = "Ruta: " + cachedJsonPath
        } else {
            jsonFilePath.text = "No hay JSON seleccionado"
        }

        btnSeleccionarJson.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo JSON"), PICK_JSON_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data!!
            val returnIntent = Intent().apply {
                putExtra("jsonUri", uri.toString())
            }
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }
}
