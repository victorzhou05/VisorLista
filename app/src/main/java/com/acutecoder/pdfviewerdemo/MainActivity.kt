package com.acutecoder.pdfviewerdemo

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acutecoder.pdfviewerdemo.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private val navigationStack = Stack<List<Elemento>>()
    private lateinit var rootElementos: List<Elemento>

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnElegirJson: Button
    private lateinit var toolbar: Toolbar
    private lateinit var btnHome: ImageButton
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: ElementoAdapter
    private lateinit var viewBinding: ActivityMainBinding

    // Preferencias para almacenar ruta al JSON cacheado
    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_PATH = "jsonFilePath"

    // Control de inactividad
    private val INACTIVITY_TIMEOUT = 30_000L
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        // Lanzar pantalla de inactividad
        startActivity(Intent(this, InactivoActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar SharedPreferences
        pref = getSharedPreferences("pref", MODE_PRIVATE)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        recyclerView = findViewById(R.id.recyclerView)
        btnElegirJson = findViewById(R.id.btnElegirJson)
        toolbar = findViewById(R.id.toolbarMain)
        btnHome = findViewById(R.id.btnHome)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Ajuste de padding para sistema de barras
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Solicitar permiso de gestión de archivos (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }

        // Click para elegir JSON (solo si no hay cache)
        btnElegirJson.setOnClickListener {
            openFilePicker()
        }

        // Botones de navegación
        btnHome.setOnClickListener {
            navigationStack.clear()
            showElementos(rootElementos)
        }
        btnBack.setOnClickListener {
            if (navigationStack.isNotEmpty()) {
                showElementos(navigationStack.pop())
            }
        }

        // Intentamos cargar JSON cacheado
        val cachedPath = pref.getString(PREF_KEY_JSON_PATH, null)
        if (cachedPath != null) {
            val cachedFile = File(cachedPath)
            if (cachedFile.exists()) {
                loadJsonFromFile(cachedFile)
                return
            } else {
                // Si no existe, limpiar pref
                pref.edit().remove(PREF_KEY_JSON_PATH).apply()
            }
        }

        // Iniciamos inactividad
        resetInactivityTimer()
    }

    override fun onResume() {
        super.onResume()
        // Si ya cargamos elementos, asegurar UI en home
        if (::rootElementos.isInitialized) {
            // Mostrar menú principal
            btnElegirJson.visibility = View.GONE
            toolbar.visibility = View.VISIBLE
            navigationStack.clear()
            showElementos(rootElementos)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK && data?.data != null) {
            data.data!!.let { uri ->
                try {
                    // Leer JSON desde picker
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val json = reader.readText()

                    // Guardar copia en interno para cache
                    val cacheFile = File(filesDir, "cached_data.json")
                    cacheFile.writeText(json)
                    // Guardar ruta en SharedPreferences
                    pref.edit().putString(PREF_KEY_JSON_PATH, cacheFile.absolutePath).apply()

                    // Cargar estructura de elementos
                    loadJsonFromFile(cacheFile)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error leyendo JSON", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Lee el JSON del archivo, oculta el selector y muestra elementos
     */
    private fun loadJsonFromFile(file: File) {
        try {
            val json = file.readText()
            val root: Elemento? = Gson().fromJson(json, Elemento::class.java)
            if (root != null) {
                rootElementos = listOf(root)
                // Ocultar selector y mostrar toolbar
                btnElegirJson.visibility = View.GONE
                toolbar.visibility = View.VISIBLE
                // Mostrar lista inicial
                showElementos(rootElementos)
                // Iniciar temporizador
                resetInactivityTimer()
            } else {
                Toast.makeText(this, "JSON inválido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando JSON cacheado", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showElementos(elementos: List<Elemento>) {
        adapter = ElementoAdapter(elementos, object : ElementoAdapter.OnElementoClickListener {
            override fun onElementoClick(elemento: Elemento) {
                if (elemento.type == "curso" && !elemento.subelementos.isNullOrEmpty()) {
                    navigationStack.push(adapter.elementos)
                    showElementos(elemento.subelementos!!)
                } else if (elemento.type == "documento") {
                    openPdf(elemento.nombre ?: return)
                }
            }
        })
        recyclerView.adapter = adapter
        updateNavButtons()
    }

    private fun updateNavButtons() {
        if (navigationStack.isEmpty()) {
            btnHome.visibility = View.GONE
            btnBack.visibility = View.GONE
        } else {
            btnHome.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE
        }
    }

    private fun openPdf(nombreArchivo: String) {
        val pdfFile = File(Environment.getExternalStorageDirectory(), "ArchivosPdf/$nombreArchivo.pdf")
        if (!pdfFile.exists()) {
            Toast.makeText(this, "Archivo no encontrado: $nombreArchivo", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("fileName", pdfFile.nameWithoutExtension)
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

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacks(inactivityRunnable)
    }
}
