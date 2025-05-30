package com.acutecoder.pdfviewerdemo

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
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
import java.io.InputStreamReader
import java.io.File
import java.util.Stack

import com.acutecoder.pdfviewerdemo.utils.setFullscreen


class MainActivity : AppCompatActivity() {

    private val PICK_JSON_FILE = 1001
    private val navigationStack = Stack<List<Elemento>>()
    private val titleStack = Stack<String>()

    private lateinit var rootElementos: List<Elemento>

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnElegirJson: Button
    private lateinit var toolbar: Toolbar
    private lateinit var btnHome: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnConfig: ImageButton
    private lateinit var tituloToolbar: TextView

    private lateinit var adapter: ElementoAdapter
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_URI = "jsonFileUri"

    private val INACTIVITY_TIMEOUT = 30_000L
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        startActivity(Intent(this, InactivoActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pref = getSharedPreferences("pref", MODE_PRIVATE)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setFullscreen(true)

        recyclerView = findViewById(R.id.recyclerView)
        btnElegirJson = findViewById(R.id.btnElegirJson)
        toolbar = findViewById(R.id.toolbarMain)
        btnHome = findViewById(R.id.btnHome)
        btnBack = findViewById(R.id.btnBack)
        tituloToolbar = findViewById(R.id.toolbarTitle)
        btnConfig = findViewById(R.id.btnConfig)

        recyclerView.layoutManager = LinearLayoutManager(this)
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.container) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }

        btnElegirJson.setOnClickListener { openFilePicker() }

        btnHome.setOnClickListener {
            if (titleStack.isEmpty()) return@setOnClickListener // Desactiva botón Home en pantalla inicial
            navigationStack.clear()
            titleStack.clear()
            showElementos(rootElementos)
        }

        btnBack.setOnClickListener {
            if (navigationStack.isEmpty()) return@setOnClickListener // Desactiva botón Atrás en pantalla inicial
            onBackPressed()
        }

        btnConfig.setOnClickListener {
            startActivityForResult(Intent(this, ConfigActivity::class.java), PICK_JSON_FILE)
        }

        val cachedUri = pref.getString(PREF_KEY_JSON_URI, null)
        if (cachedUri != null) {
            try {
                val uri = Uri.parse(cachedUri)
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                loadJsonFromUri(uri)
                return
            } catch (e: Exception) {
                pref.edit().remove(PREF_KEY_JSON_URI).apply()
                Toast.makeText(this, "Error leyendo el JSON guardado", Toast.LENGTH_SHORT).show()
            }
        }

        resetInactivityTimer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK) {
            val uriString = data?.getStringExtra("jsonUri") ?: return
            val uri = Uri.parse(uriString)

            pref.edit().putString(PREF_KEY_JSON_URI, uriString).apply()
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            try {
                loadJsonFromUri(uri)
            } catch (e: Exception) {
                Toast.makeText(this, "Error leyendo JSON seleccionado", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadJsonFromUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                val root = Gson().fromJson(sb.toString(), Elemento::class.java)
                if (root != null) {
                    rootElementos = listOf(root)
                    btnElegirJson.visibility = View.GONE
                    toolbar.visibility = View.VISIBLE
                    showElementos(rootElementos)
                    resetInactivityTimer()
                } else Toast.makeText(this, "JSON inválido", Toast.LENGTH_SHORT).show()
            }
        } ?: throw Exception("No se pudo abrir flujo para $uri")
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_JSON_FILE)
    }

    private fun showElementos(elementos: List<Elemento>) {
        adapter = ElementoAdapter(elementos, object : ElementoAdapter.OnElementoClickListener {
            override fun onElementoClick(elemento: Elemento) {
                if (elemento.type == "curso" && !elemento.subelementos.isNullOrEmpty()) {
                    navigationStack.push(adapter.elementos)
                    titleStack.push(elemento.nombre ?: "Curso sin nombre")
                    showElementos(elemento.subelementos!!)
                } else if (elemento.type == "documento") openPdf(elemento.nombre ?: return)
            }
        })
        tituloToolbar.text = if (titleStack.isEmpty()) "Inicio" else titleStack.peek()
        recyclerView.adapter = adapter
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
            titleStack.pop()
            showElementos(navigationStack.pop())
        } else {
            // Evita acción por defecto si estamos en el inicio
            // super.onBackPressed() se omite aquí
        }
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
