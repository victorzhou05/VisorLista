package com.acutecoder.pdfviewerdemo

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.URLUtil
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acutecoder.pdfviewerdemo.databinding.ActivityMainBinding
import com.acutecoder.pdfviewerdemo.databinding.UrlDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Stack

class
MainActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private val navigationStack = Stack<List<Elemento>>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnElegirJson: Button

    private lateinit var adapter: ElementoAdapter

    private lateinit var view: ActivityMainBinding
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pref = getSharedPreferences("pref", MODE_PRIVATE)

        view = ActivityMainBinding.inflate(layoutInflater)
        setContentView(view.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerView)
        btnElegirJson = findViewById(R.id.btnElegirJson)

        recyclerView.layoutManager = LinearLayoutManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(view.container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        btnElegirJson.setOnClickListener { openFilePicker()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val json = reader.readText()

                    val root = Gson().fromJson(json, Elemento::class.java)
                    if (root != null) {
                        showElementos(listOf(root))
                    } else {
                        Toast.makeText(this, "JSON inválido", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error leyendo JSON", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showElementos(elementos: List<Elemento>) {
        adapter = ElementoAdapter(elementos, object : ElementoAdapter.OnElementoClickListener {
            override fun onElementoClick(elemento: Elemento) {
                if (elemento.type == "documento") {
                    openPdf(elemento.nombre ?: return)
                } else if (elemento.type == "curso" && !elemento.subelementos.isNullOrEmpty()) {
                    navigationStack.push(adapterElementos())
                    showElementos(elemento.subelementos!!)
                }
            }
        })
        recyclerView.adapter = adapter
    }

    private fun adapterElementos(): List<Elemento> {
        return (recyclerView.adapter as ElementoAdapter).elementos
    }

    private fun openPdf(nombreArchivo: String) {
        val pdfFile = File(Environment.getExternalStorageDirectory(), "ArchivosPdf/$nombreArchivo.pdf")
        if (!pdfFile.exists()) {
            Toast.makeText(this, "Archivo no encontrado: $nombreArchivo", Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(
            Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("fileName", pdfFile.name.substringBefore(".pdf"))
                putExtra("filePath", pdfFile.absolutePath)
            }
        )

    }


    override fun onBackPressed() {
        if (navigationStack.isNotEmpty()) {
            showElementos(navigationStack.pop())
        } else {
            super.onBackPressed()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo JSON"), PICK_JSON_FILE)
    }
}
