package com.acutecoder.visoreducativo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private lateinit var btnSeleccionarJson: Button
    private lateinit var btnBack: ImageButton
    private lateinit var jsonFilePath: TextView

    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_URI = "jsonFileUri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        pref = getSharedPreferences("pref", MODE_PRIVATE)

        btnSeleccionarJson = findViewById(R.id.btnSeleccionarJson)
        btnBack = findViewById(R.id.btnBack)
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

        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!


            val returnIntent = Intent().apply {
                putExtra("jsonUri", uri.toString())
            }
            setResult(Activity.RESULT_OK, returnIntent)

            finish()
        }
    }
}
