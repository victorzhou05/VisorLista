package com.acutecoder.pdfviewerdemo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private lateinit var btnSeleccionarJson: Button
    private lateinit var jsonFilePath: TextView

    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_URI = "jsonUri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        pref = getSharedPreferences("pref", MODE_PRIVATE)

        btnSeleccionarJson = findViewById(R.id.btnSeleccionarJson)
        jsonFilePath = findViewById(R.id.rutaJson)

        // Mostrar la URI guardada si existe
        val cachedJsonUri = pref.getString(PREF_KEY_JSON_URI, null)
        if (cachedJsonUri != null) {
            jsonFilePath.text = "Ruta: $cachedJsonUri"
        } else {
            jsonFilePath.text = "No hay JSON seleccionado"
        }

        btnSeleccionarJson.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, PICK_JSON_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!

            // Guardar permiso persistente
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, "Error al obtener permisos para el archivo", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                return
            }

            // Guardar la URI en SharedPreferences
            pref.edit().putString(PREF_KEY_JSON_URI, uri.toString()).apply()

            // Mostrar la URI en el TextView
            jsonFilePath.text = "Ruta: $uri"

            // Devolver resultado si se necesita
            val returnIntent = Intent().apply {
                putExtra("jsonUri", uri.toString())
            }
            setResult(Activity.RESULT_OK, returnIntent)

            finish()
        }
    }
}
